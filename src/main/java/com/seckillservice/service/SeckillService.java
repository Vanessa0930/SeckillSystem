package com.seckillservice.service;

import com.seckillservice.common.models.CustomResponse;
import com.seckillservice.common.models.Inventory;
import com.seckillservice.common.models.Transaction;
import com.seckillservice.common.ratelimit.RedisTokenLimiter;
import com.seckillservice.handler.InventoryHandler;
import com.seckillservice.handler.TransactionHandler;
import com.seckillservice.utils.redis.RedisPoolUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static com.seckillservice.common.models.CustomResponse.FAILED;
import static com.seckillservice.common.models.CustomResponse.SUCCESS;
import static com.seckillservice.utils.constants.ACCESS_DENIED;
import static com.seckillservice.utils.constants.FAILED_RESULT;
import static com.seckillservice.utils.constants.INVENTORY_ITEM;
import static com.seckillservice.utils.constants.OUT_OF_STOCK;
import static com.seckillservice.utils.constants.SUCCESS_RESULT;

@RestController
public class SeckillService {
    // TODO: Assume there is only one item for bidding at one time.
    @Autowired
    private InventoryHandler inventoryService;
    @Autowired
    private TransactionHandler transactionService;
    private static final Logger log = LogManager.getLogger(SeckillService.class);

    public SeckillService() {
        System.out.println("***** " + this.hashCode());
    }

    @PostMapping(value = "/submitRequest")
    @ResponseBody
    public CustomResponse submitRequest(@RequestBody String inventoryId) {
        try {
            String orderId = requestHelper(inventoryId);
            return new CustomResponse(SUCCESS, String.format(SUCCESS_RESULT, orderId));
        } catch (IllegalArgumentException e) {
            return new CustomResponse(FAILED, String.format(OUT_OF_STOCK, inventoryId));
        } catch (Exception e) {
            e.printStackTrace();
            return new CustomResponse(FAILED, FAILED_RESULT);
        }
    }

    /**
     * Use Redis to construct rate limiter
     * @param inventoryId
     * @return
     */
    @PostMapping(value = "/submitRequestWithLimit")
    @ResponseBody
    public CustomResponse submitRequestWithLimit(@RequestBody String inventoryId) {
        try {
            if (RedisTokenLimiter.canGetAccess()) {
                String orderId = requestHelper(inventoryId);
                return new CustomResponse(SUCCESS, String.format(SUCCESS_RESULT, orderId));
            }
        } catch (IllegalArgumentException e) {
            return new CustomResponse(FAILED, String.format(OUT_OF_STOCK, inventoryId));
        } catch (Exception e) {
            e.printStackTrace();
            return new CustomResponse(FAILED, FAILED_RESULT);
        }
        return new CustomResponse(FAILED, ACCESS_DENIED);
    }

    /**
     * In addition to rate limiter, also leverage Redis to construct cache layer ahead of
     * service and ensure data within cache and database are consistent.
     * @param inventoryId
     * @return
     */
    @PostMapping(value = "/submitRequestWithLimitAndCache")
    @ResponseBody
    public CustomResponse submitRequestWithLimitAndCache(@RequestBody String inventoryId) {
        try {
            if (!RedisTokenLimiter.canGetAccess()) {
                return new CustomResponse(FAILED, ACCESS_DENIED);
            }

            // Read inventory information from cache
            Inventory inventory;
            List<String> cachedInfo = RedisPoolUtils.listAllElements(INVENTORY_ITEM + inventoryId);
            if (cachedInfo.isEmpty()) {
                // query directly from database and update cache
                log.info("No such key {} in cache", INVENTORY_ITEM + inventoryId);
                Optional<Inventory> queryRes = inventoryService.getInventory(inventoryId);
                if (!queryRes.isPresent()) {
                    // TODO: customize response for 404 items
                    throw new RuntimeException("Cannot find available item in the inventory.");
                }
                inventory = queryRes.get();
                RedisPoolUtils.addToListHead(INVENTORY_ITEM + inventoryId, inventory.getName(),
                        String.valueOf(inventory.getCount()), String.valueOf(inventory.getSales()),
                        String.valueOf(inventory.getVersion()));
            } else {
                // grab data from cache
                log.info("Successfully get key {} from cache", INVENTORY_ITEM + inventoryId);
                inventory = new Inventory();
                inventory.setId(inventoryId);
                inventory.setName(cachedInfo.get(1));
                inventory.setCount(Integer.valueOf(cachedInfo.get(2)));
                inventory.setSales(Integer.valueOf(cachedInfo.get(3)));
                inventory.setVersion(Integer.valueOf(cachedInfo.get(4)));
            }

            if (inventory.getCount() < 1) {
                return new CustomResponse(FAILED, String.format(OUT_OF_STOCK, inventoryId));
            }

            // Put transaction and delete old cached data
            inventoryService.updateInventoryOptimistically(inventory);
            RedisPoolUtils.del(INVENTORY_ITEM + inventoryId);
            log.info("Clean up key {} from cache", INVENTORY_ITEM + inventoryId);

            Transaction res = transactionService.createTransaction(inventory.getId());

            log.info("Successfully created transaction {} for inventory {}",
                    res.getId(), inventoryId);
            return new CustomResponse(SUCCESS, String.format(SUCCESS_RESULT, res.getId()));
        } catch (Exception e) {
            log.error("Failed to complete request for inventory {}", inventoryId, e);
            return new CustomResponse(FAILED, FAILED_RESULT);
        }
    }

    private String requestHelper(String inventoryId) throws IllegalAccessException {
        Optional<Inventory> queryRes = inventoryService.getInventory(inventoryId);
        if (!queryRes.isPresent() || queryRes.get().getCount() < 1) {
            throw new IllegalArgumentException("Cannot find available item in the inventory.");
        }

        Inventory inventory = queryRes.get();
        inventoryService.updateInventoryOptimistically(inventory);

        Transaction res = transactionService.createTransaction(inventory.getId());
        return res.getId();
    }
}

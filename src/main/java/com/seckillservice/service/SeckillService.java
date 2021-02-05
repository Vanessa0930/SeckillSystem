package com.seckillservice.service;

import com.seckillservice.common.models.CustomResponse;
import com.seckillservice.common.models.Inventory;
import com.seckillservice.common.models.Transaction;
import com.seckillservice.common.ratelimit.RedisTokenLimiter;
import com.seckillservice.handler.InventoryHandler;
import com.seckillservice.handler.TransactionHandler;
import com.seckillservice.utils.redis.RedisPool;
import com.seckillservice.utils.redis.RedisPoolUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import java.sql.SQLException;
import java.util.Optional;

import static com.seckillservice.common.models.CustomResponse.FAILED;
import static com.seckillservice.common.models.CustomResponse.SUCCESS;
import static com.seckillservice.utils.constants.ACCESS_DENIED;
import static com.seckillservice.utils.constants.FAILED_RESULT;
import static com.seckillservice.utils.constants.INVENTORY_COUNT;
import static com.seckillservice.utils.constants.INVENTORY_NAME;
import static com.seckillservice.utils.constants.INVENTORY_SALES;
import static com.seckillservice.utils.constants.INVENTORY_VERSION;
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

            // get inventory info from cache
            // FIXME: Assume cache has been preloaded inventory information from database
            int count = Integer.valueOf(RedisPoolUtils.get(INVENTORY_COUNT + inventoryId));
            int sales = Integer.valueOf(RedisPoolUtils.get(INVENTORY_SALES + inventoryId));
            int version = Integer.valueOf(RedisPoolUtils.get(INVENTORY_VERSION + inventoryId));
            String name = RedisPoolUtils.get(INVENTORY_NAME + inventoryId);
            log.info("Successfully get keys for item {} from cache", inventoryId);

            Inventory item = new Inventory(inventoryId, name, count, sales, version);
            if (item.getCount() < 1) {
                return new CustomResponse(FAILED, String.format(OUT_OF_STOCK, inventoryId));
            }
            Transaction res = updateInventoryAcid(item);

            return new CustomResponse(SUCCESS, String.format(SUCCESS_RESULT, res.getId()));
        } catch (Exception e) {
            log.error("Failed to complete request for inventory {}", inventoryId, e);
            return new CustomResponse(FAILED, FAILED_RESULT);
        }
    }

    private synchronized Transaction updateInventoryAcid(Inventory item) throws SQLException {
        try {
            inventoryService.updateInventoryOptimistically(item);
            log.info("Update sales record in case for inventory {} version {}",
                    item.getId(), item.getVersion());
            updateInventorySalesInCache(item.getId());
            log.info("Create transaction");
            Transaction res = transactionService.createTransaction(item.getId());
            log.info("Successfully created transaction {} for inventory {} version {}",
                    res.getId(), item.getId(), item.getVersion() - 1);
            return res;
        } catch (Exception e) {
            log.error("Failed to update database inventory sales record", e);
            throw e;
        }
    }

    private void updateInventorySalesInCache(String id) {
        Jedis jedis = null;
        try {
            jedis = RedisPool.getJedis();
            redis.clients.jedis.Transaction transaction = jedis.multi();
            RedisPoolUtils.decr(INVENTORY_COUNT + id);
            RedisPoolUtils.incr(INVENTORY_SALES + id);
            RedisPoolUtils.incr(INVENTORY_VERSION + id);
            transaction.exec();
        } catch (Exception e) {
            log.error("Failed to update cache for inventory {}", id);
            throw new RuntimeException("Failed to update cache");
        } finally {
            RedisPool.closeJedis(jedis);
        }
    }

    private String requestHelper(String inventoryId) throws IllegalAccessException, SQLException {
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

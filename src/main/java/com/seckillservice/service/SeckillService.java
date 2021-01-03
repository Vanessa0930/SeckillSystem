package main.java.com.seckillservice.service;

import main.java.com.seckillservice.common.models.Inventory;
import main.java.com.seckillservice.common.models.Transaction;
import main.java.com.seckillservice.common.ratelimit.RedisTokenLimiter;
import main.java.com.seckillservice.handler.InventoryHandler;
import main.java.com.seckillservice.handler.TransactionHandler;

import java.util.Optional;

import static main.java.com.seckillservice.utils.constants.ACCESS_DENIED;
import static main.java.com.seckillservice.utils.constants.FAILED_RESULT;
import static main.java.com.seckillservice.utils.constants.OUT_OF_STOCK;
import static main.java.com.seckillservice.utils.constants.SUCCESS_RESULT;

public class SeckillService {
    private static SeckillService instance;
    private static InventoryHandler inventoryService;
    private static TransactionHandler transactionService;

    private SeckillService() {
        inventoryService = new InventoryHandler();
        transactionService = new TransactionHandler();
    }

    public static synchronized SeckillService getInstance() {
        if (instance == null) {
            instance = new SeckillService();
        }
        return instance;
    }

    /**
     * Submit a request for selected item and returns the transaction id if succeeded
     * @param inventoryId the inventory id for selected item
     *
     * TODO: Assume there is only one item for bidding at one time.
     * @return corresponding transaction id if succeeded
     */
    public synchronized String submitRequest(String inventoryId) {
        try {
            String orderId = requestHelper(inventoryId);
            return String.format(SUCCESS_RESULT, orderId);
        } catch(IllegalArgumentException e) {
            return String.format(OUT_OF_STOCK, inventoryId);
        } catch (Exception e) {
            e.printStackTrace();
            return String.format(FAILED_RESULT);
        }
    }

    /**
     * Submit a bidding request for selected item and returns transaction id if succeeded.
     * This method is integrated with Redis pool to ensure throughput is under control.
     *
     * TODO: Assume there is only one item for bidding at one time.
     *
     * @param inventoryId the inventory id for selected item
     * @return corresponding transaction id if succeeded.
     */
    public synchronized String submitRequestWithRedis(String inventoryId) {
        try {
            if (RedisTokenLimiter.canGetAccess()) {
                String orderId = requestHelper(inventoryId);
                return String.format(SUCCESS_RESULT, orderId);
            }
        } catch (IllegalArgumentException e) {
            return String.format(OUT_OF_STOCK, inventoryId);
        } catch (Exception e) {
            e.printStackTrace();
            return String.format(FAILED_RESULT);
        }
        return ACCESS_DENIED;
    }

    private String requestHelper(String inventoryId) throws IllegalAccessException {
        Optional<Inventory> queryRes = inventoryService.getInventory(inventoryId);
        if (!queryRes.isPresent() || queryRes.get().getCount() < 1) {
            throw new IllegalAccessException("Cannot find available item in the inventory.");
        }

        Inventory inventory = queryRes.get();
        inventoryService.updateInventoryOptimistically(inventory);

        Transaction res = transactionService.createTransaction(inventory.getId());
        return res.getId();
    }
}

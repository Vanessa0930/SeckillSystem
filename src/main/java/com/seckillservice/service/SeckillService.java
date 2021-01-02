package main.java.com.seckillservice.service;

import main.java.com.seckillservice.common.models.Inventory;
import main.java.com.seckillservice.common.models.Transaction;
import main.java.com.seckillservice.common.ratelimit.RedisTokenLimiter;
import main.java.com.seckillservice.handler.InventoryHandler;
import main.java.com.seckillservice.handler.TransactionHandler;

import java.util.Optional;

public class SeckillService {
    private static InventoryHandler inventoryService = InventoryHandler.getInstance();
    private static TransactionHandler transactionService = TransactionHandler.getInstance();

    private static final String SUCCESS_RESULT = "Successfully created the order. Transaction ID: %s";
    private static final String FAILED_RESULT = "Failed to create the order. Please try again.";

    /**
     * Submit a request for selected item and returns the transaction id if succeeded
     * @param inventoryId the inventory id for selected item
     *
     * TODO: Assume there is only one item for bidding at one time.
     * @return corresponding transaction id if succeeded
     */
    public String submitRequest(String inventoryId) {
        try {
            String orderId = requestHelper(inventoryId);
            return String.format(SUCCESS_RESULT, orderId);
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
    public String submitRequestWithRedis(String inventoryId) {
        try {
            if (RedisTokenLimiter.canGetAccess()) {
                String orderId = requestHelper(inventoryId);
                return String.format(SUCCESS_RESULT, orderId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return String.format(FAILED_RESULT);
        }
        return "Request being throttled.";
    }

    private String requestHelper(String inventoryId) {
        Optional<Inventory> queryRes = inventoryService.getInventory(inventoryId);
        if (!queryRes.isPresent() || queryRes.get().getCount() < 1) {
            throw new RuntimeException("Selected item is out of stock");
        }

        Inventory inventory = queryRes.get();
        inventoryService.updateInventoryOptimistically(inventory);

        Transaction res = transactionService.createTransaction(inventory.getId());
        return res.getId();
    }
}

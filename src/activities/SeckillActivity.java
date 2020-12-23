package activities;

import handler.InventoryHandler;
import handler.TransactionHandler;
import models.Inventory;
import models.Transaction;

import java.util.Optional;

public class SeckillActivity {
    private final InventoryHandler inventoryService = InventoryHandler.getInstance();
    private final TransactionHandler transactionService = TransactionHandler.getInstance();

    /**
     * Submit a bidding request for selected item and returns the transaction id if succeeded
     * @param inventoryId the inventory id for selected item
     * @return corresponding transaction id if succeeded
     */
    public String submitBiddingRequest(String inventoryId) {
        try {
            Optional<Inventory> queryRes = inventoryService.getInventory(inventoryId);
            if (!queryRes.isPresent() || queryRes.get().getCount() < 1) {
                throw new RuntimeException("Selected item is out of stock");
            }

            Inventory inventory = queryRes.get();
            inventoryService.updateInventory(inventory);

            Transaction res = transactionService.createTransaction(inventory.getId());
            return res.getId();
        } catch (Exception e) {
            throw new RuntimeException("Cannot complete the bidding request: ", e);
        }
    }
}

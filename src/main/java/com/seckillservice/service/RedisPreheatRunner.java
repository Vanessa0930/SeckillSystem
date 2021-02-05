package com.seckillservice.service;

import com.seckillservice.common.models.Inventory;
import com.seckillservice.handler.InventoryHandler;
import com.seckillservice.utils.redis.RedisPoolUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.seckillservice.utils.constants.INVENTORY_COUNT;
import static com.seckillservice.utils.constants.INVENTORY_NAME;
import static com.seckillservice.utils.constants.INVENTORY_SALES;
import static com.seckillservice.utils.constants.INVENTORY_VERSION;

@Component
public class RedisPreheatRunner implements ApplicationRunner {

    @Autowired
    private InventoryHandler inventoryHandler;
    private static final Logger log = LogManager.getLogger(SeckillService.class);

    @Override
    public void run(ApplicationArguments args) {
        log.info("Preload all inventory information to cache");
        try {
            List<String> inventoryIds = inventoryHandler.listAllInventoryIds();
            for (String id : inventoryIds) {
                Optional<Inventory> queriedRes = inventoryHandler.getInventory(id);
                if (queriedRes.isPresent()) {
                    Inventory item = queriedRes.get();
                    // delete cached data and add latest data
                    RedisPoolUtils.del(INVENTORY_NAME + id);
                    RedisPoolUtils.del(INVENTORY_COUNT + id);
                    RedisPoolUtils.del(INVENTORY_SALES + id);
                    RedisPoolUtils.del(INVENTORY_VERSION + id);

                    RedisPoolUtils.set(INVENTORY_NAME + id, item.getName());
                    RedisPoolUtils.set(INVENTORY_COUNT + id, String.valueOf(item.getCount()));
                    RedisPoolUtils.set(INVENTORY_SALES + id, String.valueOf(item.getSales()));
                    RedisPoolUtils.set(INVENTORY_VERSION + id, String.valueOf(item.getVersion()));
                }
            }
        } catch (Exception e) {
            log.error("Failed to complete preloading inventories", e);
        }
    }
}

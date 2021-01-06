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
import static com.seckillservice.utils.constants.INVENTORY_SALE;
import static com.seckillservice.utils.constants.INVENTORY_VERSION;

@Component
public class RedisPreheatRunner implements ApplicationRunner {

    @Autowired
    private InventoryHandler inventoryHandler;
    private static final Logger log = LogManager.getLogger(SeckillService.class);

    @Override
    public void run(ApplicationArguments args) {
        log.info("Preload all inventory information to cache");
        List<String> inventoryIds = inventoryHandler.listAllInventoryIds();
        for (String id : inventoryIds) {
            Optional<Inventory> queriedRes = inventoryHandler.getInventory(id);
            if (queriedRes.isPresent()) {
                Inventory item = queriedRes.get();
                // delete cached data and add latest data
                RedisPoolUtils.del(INVENTORY_NAME + item.getName());
                RedisPoolUtils.del(INVENTORY_COUNT + item.getCount());
                RedisPoolUtils.del(INVENTORY_SALE + item.getSales());
                RedisPoolUtils.del(INVENTORY_VERSION + item.getVersion());

                RedisPoolUtils.set(INVENTORY_NAME + item.getId(), item.getName());
                RedisPoolUtils.set(INVENTORY_COUNT + item.getId(), String.valueOf(item.getCount()));
                RedisPoolUtils.set(INVENTORY_SALE + item.getId(), String.valueOf(item.getSales()));
                RedisPoolUtils.set(INVENTORY_VERSION + item.getId(), String.valueOf(item.getVersion()));
            }
        }
    }
}

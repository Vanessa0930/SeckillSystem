package com.seckillservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.seckillservice")
public class SeckillServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(SeckillServiceApp.class, args);
//
//        InventoryHandler handler = new InventoryHandler();
//        Inventory item = new Inventory("2", "Item2", 8, 2, 3);
//        try {
//            handler.updateInventoryOptimistically(item);
//            System.out.println(item.prettyPrintRecord());
//        } catch (SQLException throwables) {
//            throwables.printStackTrace();
//        }
    }
}

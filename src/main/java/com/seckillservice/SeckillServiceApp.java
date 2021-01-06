package com.seckillservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.seckillservice")
public class SeckillServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(SeckillServiceApp.class, args);
    }
}

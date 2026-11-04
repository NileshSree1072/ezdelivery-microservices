package com.ezdelivery.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.ezdelivery.common.kafka.KafkaCommonConfig;

@SpringBootApplication
@Import(KafkaCommonConfig.class)
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}

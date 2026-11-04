package com.ezdelivery.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.ezdelivery.common.kafka.KafkaCommonConfig;

@SpringBootApplication
@Import(KafkaCommonConfig.class)
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}

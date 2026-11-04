package com.ezdelivery.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.ezdelivery.common.kafka.KafkaCommonConfig;

@SpringBootApplication
@Import(KafkaCommonConfig.class)
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

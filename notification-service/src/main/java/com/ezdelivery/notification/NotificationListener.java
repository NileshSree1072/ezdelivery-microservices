package com.ezdelivery.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ezdelivery.common.event.Events.OrderCompleted;
import com.ezdelivery.common.event.Topics;

/**
 * Reacts to the end of the saga. Logs the message for now; an email/SMS provider
 * can be plugged in here without touching any other service.
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    @KafkaListener(topics = Topics.ORDER_COMPLETED)
    public void onOrderCompleted(OrderCompleted event) {
        if ("CONFIRMED".equals(event.status())) {
            log.info("[NOTIFY {}] Your EzDelivery order {} is confirmed. Total: Rs. {}",
                    event.customerEmail(), event.orderId(), event.totalAmount());
        } else {
            log.info("[NOTIFY {}] Sorry, your EzDelivery order {} was cancelled: {}",
                    event.customerEmail(), event.orderId(), event.reason());
        }
    }
}

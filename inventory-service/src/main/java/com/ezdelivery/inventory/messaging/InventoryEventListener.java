package com.ezdelivery.inventory.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ezdelivery.common.event.Events.OrderCreated;
import com.ezdelivery.common.event.Events.PaymentFailed;
import com.ezdelivery.common.event.Events.PaymentSucceeded;
import com.ezdelivery.common.event.Events.StockRejected;
import com.ezdelivery.common.event.Events.StockReserved;
import com.ezdelivery.common.event.Topics;
import com.ezdelivery.inventory.model.StockReservation;
import com.ezdelivery.inventory.service.InventoryService;

@Component
public class InventoryEventListener {

    private final InventoryService inventory;
    private final KafkaTemplate<String, Object> kafka;

    public InventoryEventListener(InventoryService inventory, KafkaTemplate<String, Object> kafka) {
        this.inventory = inventory;
        this.kafka = kafka;
    }

    /**
     * The reply is published after the DB transaction commits. If publishing fails, the listener
     * throws, Kafka redelivers, and the idempotent reserve() returns the stored result to re-publish.
     */
    @KafkaListener(topics = Topics.ORDER_CREATED)
    public void onOrderCreated(OrderCreated event) {
        StockReservation reservation = inventory.reserve(event);
        String key = event.orderId().toString();
        switch (reservation.getStatus()) {
            case RESERVED -> kafka.send(Topics.STOCK_RESERVED, key,
                    new StockReserved(event.orderId(), event.customerEmail(), event.totalAmount())).join();
            case REJECTED -> kafka.send(Topics.STOCK_REJECTED, key,
                    new StockRejected(event.orderId(), reservation.getReason())).join();
            default -> {
                // Already released or committed: the saga has moved on, nothing to reply.
            }
        }
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED)
    public void onPaymentFailed(PaymentFailed event) {
        inventory.release(event.orderId());
    }

    @KafkaListener(topics = Topics.PAYMENT_SUCCEEDED)
    public void onPaymentSucceeded(PaymentSucceeded event) {
        inventory.commit(event.orderId());
    }
}

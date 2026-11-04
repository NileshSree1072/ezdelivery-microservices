package com.ezdelivery.order.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ezdelivery.common.event.Events.OrderCompleted;
import com.ezdelivery.common.event.Events.PaymentFailed;
import com.ezdelivery.common.event.Events.PaymentSucceeded;
import com.ezdelivery.common.event.Events.StockRejected;
import com.ezdelivery.common.event.Events.StockReserved;
import com.ezdelivery.common.event.Topics;
import com.ezdelivery.order.model.CustomerOrder;
import com.ezdelivery.order.model.OrderStatus;
import com.ezdelivery.order.service.OrderService;

/** The order service orchestrates nothing; it reacts to the outcome of each saga step (choreography). */
@Component
public class OrderSagaListener {

    private final OrderService orders;
    private final KafkaTemplate<String, Object> kafka;

    public OrderSagaListener(OrderService orders, KafkaTemplate<String, Object> kafka) {
        this.orders = orders;
        this.kafka = kafka;
    }

    @KafkaListener(topics = Topics.STOCK_RESERVED)
    public void onStockReserved(StockReserved event) {
        orders.transition(event.orderId(), OrderStatus.STOCK_RESERVED, null);
    }

    @KafkaListener(topics = Topics.STOCK_REJECTED)
    public void onStockRejected(StockRejected event) {
        orders.transition(event.orderId(), OrderStatus.CANCELLED, event.reason()).ifPresent(this::publishCompleted);
    }

    @KafkaListener(topics = Topics.PAYMENT_SUCCEEDED)
    public void onPaymentSucceeded(PaymentSucceeded event) {
        orders.transition(event.orderId(), OrderStatus.CONFIRMED, null).ifPresent(this::publishCompleted);
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED)
    public void onPaymentFailed(PaymentFailed event) {
        orders.transition(event.orderId(), OrderStatus.CANCELLED, event.reason()).ifPresent(this::publishCompleted);
    }

    private void publishCompleted(CustomerOrder order) {
        kafka.send(Topics.ORDER_COMPLETED, order.getId().toString(),
                new OrderCompleted(order.getId(), order.getCustomerEmail(), order.getStatus().name(),
                        order.getTotalAmount(), order.getFailureReason())).join();
    }
}

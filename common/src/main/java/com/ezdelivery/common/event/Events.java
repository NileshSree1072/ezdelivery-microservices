package com.ezdelivery.common.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Event contracts exchanged over Kafka in the order saga.
 *
 * <pre>
 * order.created ──► inventory ──► stock-reserved ──► payment ──► payment.succeeded ──► order CONFIRMED
 *                           └──► stock-rejected ──► order CANCELLED
 *                                                         └──► payment.failed ──► inventory releases stock
 *                                                                              └──► order CANCELLED
 * </pre>
 *
 * Every event is keyed by orderId, so all events for one order land on the same partition.
 */
public final class Events {

    private Events() {
    }

    public record OrderLineItem(String productId, int quantity, BigDecimal unitPrice) {
    }

    public record OrderCreated(UUID orderId, String customerEmail, List<OrderLineItem> items,
                               BigDecimal totalAmount) {
    }

    public record StockReserved(UUID orderId, String customerEmail, BigDecimal totalAmount) {
    }

    public record StockRejected(UUID orderId, String reason) {
    }

    public record PaymentSucceeded(UUID orderId, UUID paymentId, BigDecimal amount) {
    }

    public record PaymentFailed(UUID orderId, String reason) {
    }

    public record OrderCompleted(UUID orderId, String customerEmail, String status,
                                 BigDecimal totalAmount, String reason) {
    }
}

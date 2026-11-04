package com.ezdelivery.common.event;

import java.util.List;

public final class Topics {

    public static final String ORDER_CREATED = "order.created";
    public static final String STOCK_RESERVED = "inventory.stock-reserved";
    public static final String STOCK_REJECTED = "inventory.stock-rejected";
    public static final String PAYMENT_SUCCEEDED = "payment.succeeded";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String ORDER_COMPLETED = "order.completed";

    /** Suffix used by Spring Kafka's DeadLetterPublishingRecoverer. */
    public static final String DLT_SUFFIX = ".DLT";

    public static final List<String> ALL = List.of(
            ORDER_CREATED, STOCK_RESERVED, STOCK_REJECTED, PAYMENT_SUCCEEDED, PAYMENT_FAILED, ORDER_COMPLETED);

    private Topics() {
    }
}

package com.ezdelivery.order.model;

public enum OrderStatus {
    /** Order saved, waiting for inventory. */
    PENDING,
    /** Inventory is holding the stock, waiting for payment. */
    STOCK_RESERVED,
    /** Paid and stock committed. Final. */
    CONFIRMED,
    /** Out of stock or payment failed. Final. */
    CANCELLED;

    public boolean isFinal() {
        return this == CONFIRMED || this == CANCELLED;
    }
}

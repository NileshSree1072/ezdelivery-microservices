package com.ezdelivery.inventory.model;

public enum ReservationStatus {
    /** Stock is held for the order while payment is processed. */
    RESERVED,
    /** Not enough stock; nothing was held. */
    REJECTED,
    /** Payment failed; held stock was returned. */
    RELEASED,
    /** Payment succeeded; held stock was sold. */
    COMMITTED
}

package com.ezdelivery.order.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CustomerOrderTest {

    private CustomerOrder newOrder() {
        return new CustomerOrder("a@b.com", "A", null, null, null, null);
    }

    @Test
    void totalIsSumOfLinePrices() {
        CustomerOrder order = newOrder();
        order.addLine("p1", "Bananas", new BigDecimal("60.00"), 3);
        order.addLine("p2", "Rice", new BigDecimal("1500.00"), 1);

        assertThat(order.getTotalAmount()).isEqualByComparingTo("1680.00");
    }

    @Test
    void happyPathMovesForward() {
        CustomerOrder order = newOrder();

        assertThat(order.transitionTo(OrderStatus.STOCK_RESERVED, null)).isTrue();
        assertThat(order.transitionTo(OrderStatus.CONFIRMED, null)).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void finalStatesCannotChange() {
        CustomerOrder order = newOrder();
        order.transitionTo(OrderStatus.CANCELLED, "Out of stock");

        assertThat(order.transitionTo(OrderStatus.CONFIRMED, null)).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getFailureReason()).isEqualTo("Out of stock");
    }

    @Test
    void lateStockReservedEventDoesNotMoveConfirmedOrderBackwards() {
        CustomerOrder order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMED, null);

        assertThat(order.transitionTo(OrderStatus.STOCK_RESERVED, null)).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void repeatingTheSameStatusIsANoOp() {
        CustomerOrder order = newOrder();
        order.transitionTo(OrderStatus.STOCK_RESERVED, null);

        assertThat(order.transitionTo(OrderStatus.STOCK_RESERVED, null)).isFalse();
    }
}

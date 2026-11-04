package com.ezdelivery.inventory.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class InventoryItemTest {

    @Test
    void reserveMovesUnitsFromAvailableToReserved() {
        InventoryItem item = new InventoryItem("p1", 10);
        item.reserve(4);

        assertThat(item.getAvailableQuantity()).isEqualTo(6);
        assertThat(item.getReservedQuantity()).isEqualTo(4);
    }

    @Test
    void cannotReserveMoreThanAvailable() {
        InventoryItem item = new InventoryItem("p1", 2);

        assertThat(item.canReserve(3)).isFalse();
        assertThatThrownBy(() -> item.reserve(3)).isInstanceOf(IllegalStateException.class);
        assertThat(item.getAvailableQuantity()).isEqualTo(2);
    }

    @Test
    void releasePutsUnitsBackOnTheShelf() {
        InventoryItem item = new InventoryItem("p1", 10);
        item.reserve(4);
        item.release(4);

        assertThat(item.getAvailableQuantity()).isEqualTo(10);
        assertThat(item.getReservedQuantity()).isZero();
    }

    @Test
    void commitRemovesSoldUnits() {
        InventoryItem item = new InventoryItem("p1", 10);
        item.reserve(4);
        item.commit(4);

        assertThat(item.getAvailableQuantity()).isEqualTo(6);
        assertThat(item.getReservedQuantity()).isZero();
    }
}

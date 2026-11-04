package com.ezdelivery.inventory.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @Column(name = "product_id", length = 64)
    private String productId;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected InventoryItem() {
    }

    public InventoryItem(String productId, int availableQuantity) {
        this.productId = productId;
        this.availableQuantity = availableQuantity;
    }

    public boolean canReserve(int quantity) {
        return availableQuantity >= quantity;
    }

    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("Insufficient stock for " + productId);
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    /** Compensation: puts reserved units back on the shelf. */
    public void release(int quantity) {
        reservedQuantity -= quantity;
        availableQuantity += quantity;
    }

    /** Payment succeeded: reserved units are sold and leave the warehouse. */
    public void commit(int quantity) {
        reservedQuantity -= quantity;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public String getProductId() { return productId; }
    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public Instant getUpdatedAt() { return updatedAt; }
}

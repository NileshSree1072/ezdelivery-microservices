package com.ezdelivery.order.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Named CustomerOrder because ORDER is a reserved word in SQL and JPQL. */
@Entity
@Table(name = "orders")
public class CustomerOrder {

    @Id
    private UUID id;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "street_address")
    private String streetAddress;

    @Column(length = 100)
    private String city;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 100)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "failure_reason")
    private String failureReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic locking: two events updating the same order at once cannot overwrite each other. */
    @Version
    private long version;

    protected CustomerOrder() {
    }

    public CustomerOrder(String customerEmail, String customerName, String streetAddress,
                         String city, String postalCode, String country) {
        this.id = UUID.randomUUID();
        this.customerEmail = customerEmail;
        this.customerName = customerName;
        this.streetAddress = streetAddress;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
        this.status = OrderStatus.PENDING;
    }

    public void addLine(String productId, String productTitle, BigDecimal unitPrice, int quantity) {
        lines.add(new OrderLine(this, productId, productTitle, unitPrice, quantity));
        totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    /**
     * Applies a saga step. Events from different topics can arrive in any order
     * (e.g. payment.succeeded before stock-reserved), so only forward moves are allowed
     * and anything after a final state is ignored.
     *
     * @return true if the status changed
     */
    public boolean transitionTo(OrderStatus target, String reason) {
        if (status.isFinal() || status == target) {
            return false;
        }
        if (target == OrderStatus.STOCK_RESERVED && status != OrderStatus.PENDING) {
            return false;
        }
        status = target;
        failureReason = reason;
        return true;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getCustomerEmail() { return customerEmail; }
    public String getCustomerName() { return customerName; }
    public String getStreetAddress() { return streetAddress; }
    public String getCity() { return city; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getFailureReason() { return failureReason; }
    public List<OrderLine> getLines() { return lines; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

package com.ezdelivery.payment.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class Payment {

    public enum Status { SUCCEEDED, FAILED }

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_reference")
    private String providerReference;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Payment() {
    }

    public Payment(UUID orderId, BigDecimal amount, String provider, Status status,
                   String providerReference, String failureReason) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.amount = amount;
        this.provider = provider;
        this.status = status;
        this.providerReference = providerReference;
        this.failureReason = failureReason;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public BigDecimal getAmount() { return amount; }
    public Status getStatus() { return status; }
    public String getProvider() { return provider; }
    public String getProviderReference() { return providerReference; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
}

package com.ezdelivery.inventory.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_reservations")
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    private String reason;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationLine> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StockReservation() {
    }

    private StockReservation(UUID orderId, ReservationStatus status, String reason) {
        this.orderId = orderId;
        this.status = status;
        this.reason = reason;
    }

    public static StockReservation reserved(UUID orderId) {
        return new StockReservation(orderId, ReservationStatus.RESERVED, null);
    }

    public static StockReservation rejected(UUID orderId, String reason) {
        return new StockReservation(orderId, ReservationStatus.REJECTED, reason);
    }

    public void addLine(String productId, int quantity) {
        lines.add(new ReservationLine(this, productId, quantity));
    }

    public void markReleased() {
        status = ReservationStatus.RELEASED;
    }

    public void markCommitted() {
        status = ReservationStatus.COMMITTED;
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

    public Long getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public ReservationStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public List<ReservationLine> getLines() { return lines; }
}

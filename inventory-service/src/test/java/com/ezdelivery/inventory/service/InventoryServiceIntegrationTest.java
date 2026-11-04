package com.ezdelivery.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.ezdelivery.common.event.Events.OrderCreated;
import com.ezdelivery.common.event.Events.OrderLineItem;
import com.ezdelivery.inventory.model.ReservationStatus;
import com.ezdelivery.inventory.model.StockReservation;
import com.ezdelivery.inventory.repository.InventoryItemRepository;
import com.ezdelivery.inventory.repository.StockReservationRepository;

/** Runs against a real PostgreSQL (Flyway schema, row locks). Kafka is not needed here. */
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false"})
@Testcontainers
class InventoryServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    InventoryService inventory;

    @Autowired
    InventoryItemRepository items;

    @Autowired
    StockReservationRepository reservations;

    @BeforeEach
    void clean() {
        reservations.deleteAll();
        items.deleteAll();
    }

    private static OrderCreated order(String productId, int qty) {
        return new OrderCreated(UUID.randomUUID(), "a@b.com",
                List.of(new OrderLineItem(productId, qty, BigDecimal.TEN)), BigDecimal.TEN.multiply(BigDecimal.valueOf(qty)));
    }

    private int available(String productId) {
        return inventory.find(productId).orElseThrow().getAvailableQuantity();
    }

    @Test
    void reservesStockWhenAvailable() {
        inventory.setAvailable("apple", 10);

        StockReservation r = inventory.reserve(order("apple", 4));

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(available("apple")).isEqualTo(6);
    }

    @Test
    void rejectsWholeOrderWhenAnyLineIsShort() {
        inventory.setAvailable("apple", 10);
        inventory.setAvailable("rice", 1);
        OrderCreated order = new OrderCreated(UUID.randomUUID(), "a@b.com", List.of(
                new OrderLineItem("apple", 2, BigDecimal.ONE),
                new OrderLineItem("rice", 5, BigDecimal.ONE)), BigDecimal.TEN);

        StockReservation r = inventory.reserve(order);

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(r.getReason()).contains("rice");
        assertThat(available("apple")).as("all-or-nothing: apple untouched").isEqualTo(10);
    }

    @Test
    void duplicateEventDoesNotReserveTwice() {
        inventory.setAvailable("apple", 10);
        OrderCreated order = order("apple", 4);

        inventory.reserve(order);
        inventory.reserve(order);
        inventory.reserve(order);

        assertThat(available("apple")).isEqualTo(6);
        assertThat(reservations.count()).isEqualTo(1);
    }

    @Test
    void releaseRestoresStockAfterPaymentFailure() {
        inventory.setAvailable("apple", 10);
        OrderCreated order = order("apple", 4);
        inventory.reserve(order);

        inventory.release(order.orderId());
        inventory.release(order.orderId());   // duplicate payment.failed

        assertThat(available("apple")).isEqualTo(10);
        assertThat(inventory.find("apple").orElseThrow().getReservedQuantity()).isZero();
        assertThat(reservations.findByOrderId(order.orderId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void concurrentOrdersNeverOversellTheLastUnits() throws Exception {
        inventory.setAvailable("apple", 5);
        int buyers = 20;
        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<StockReservation>> results = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            Callable<StockReservation> buy = () -> {
                start.await();
                return inventory.reserve(order("apple", 1));
            };
            results.add(pool.submit(buy));
        }
        start.countDown();

        int reserved = 0;
        for (Future<StockReservation> f : results) {
            if (f.get().getStatus() == ReservationStatus.RESERVED) {
                reserved++;
            }
        }
        pool.shutdown();

        assertThat(reserved).as("exactly the 5 units in stock are sold").isEqualTo(5);
        assertThat(available("apple")).isZero();
    }
}

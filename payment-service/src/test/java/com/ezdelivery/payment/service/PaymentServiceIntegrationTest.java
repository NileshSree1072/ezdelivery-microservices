package com.ezdelivery.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.ezdelivery.common.event.Events.StockReserved;
import com.ezdelivery.payment.model.Payment;
import com.ezdelivery.payment.repository.PaymentRepository;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false",
        "payment.mock.decline-above=10000.00"})
@Testcontainers
class PaymentServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    PaymentService service;

    @Autowired
    PaymentRepository payments;

    @BeforeEach
    void clean() {
        payments.deleteAll();
    }

    @Test
    void chargesOrderOnce() {
        StockReserved event = new StockReserved(UUID.randomUUID(), "a@b.com", new BigDecimal("180.00"));

        Payment first = service.charge(event);
        Payment second = service.charge(event);   // redelivered event

        assertThat(first.getStatus()).isEqualTo(Payment.Status.SUCCEEDED);
        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(payments.count()).isEqualTo(1);
    }

    @Test
    void recordsDeclinedPayment() {
        Payment p = service.charge(new StockReserved(UUID.randomUUID(), "a@b.com", new BigDecimal("12000.00")));

        assertThat(p.getStatus()).isEqualTo(Payment.Status.FAILED);
        assertThat(p.getFailureReason()).contains("declined");
    }

    @Test
    void databaseRejectsASecondPaymentForTheSameOrder() {
        UUID orderId = UUID.randomUUID();
        payments.saveAndFlush(new Payment(orderId, BigDecimal.TEN, "MOCK", Payment.Status.SUCCEEDED, "ref1", null));

        assertThatThrownBy(() -> payments.saveAndFlush(
                new Payment(orderId, BigDecimal.TEN, "MOCK", Payment.Status.SUCCEEDED, "ref2", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

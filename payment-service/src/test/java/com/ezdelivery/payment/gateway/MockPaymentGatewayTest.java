package com.ezdelivery.payment.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ezdelivery.payment.gateway.PaymentGateway.ChargeResult;

class MockPaymentGatewayTest {

    private final MockPaymentGateway gateway = new MockPaymentGateway(new BigDecimal("10000.00"));

    @Test
    void approvesAmountsUpToTheLimit() {
        ChargeResult result = gateway.charge(UUID.randomUUID(), new BigDecimal("10000.00"), "a@b.com");

        assertThat(result.success()).isTrue();
        assertThat(result.reference()).startsWith("mock_");
    }

    @Test
    void declinesAmountsAboveTheLimit() {
        ChargeResult result = gateway.charge(UUID.randomUUID(), new BigDecimal("10000.01"), "a@b.com");

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).contains("declined");
    }
}

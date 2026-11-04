package com.ezdelivery.payment.gateway;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Local/test provider. Approves every charge up to a limit and declines above it,
 * which makes the payment-failure path of the saga easy to demonstrate.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    private final BigDecimal declineAbove;

    public MockPaymentGateway(@Value("${payment.mock.decline-above:10000.00}") BigDecimal declineAbove) {
        this.declineAbove = declineAbove;
    }

    @Override
    public String name() {
        return "MOCK";
    }

    @Override
    public ChargeResult charge(UUID orderId, BigDecimal amount, String customerEmail) {
        if (amount.compareTo(declineAbove) > 0) {
            return ChargeResult.declined("Card declined: amount %s exceeds limit %s".formatted(amount, declineAbove));
        }
        return ChargeResult.approved("mock_" + UUID.randomUUID());
    }
}

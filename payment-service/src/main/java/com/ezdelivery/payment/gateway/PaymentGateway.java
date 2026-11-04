package com.ezdelivery.payment.gateway;

import java.math.BigDecimal;
import java.util.UUID;

/** Abstraction over the payment provider, so Stripe or a test double can be swapped in. */
public interface PaymentGateway {

    String name();

    ChargeResult charge(UUID orderId, BigDecimal amount, String customerEmail);

    record ChargeResult(boolean success, String reference, String failureReason) {

        public static ChargeResult approved(String reference) {
            return new ChargeResult(true, reference, null);
        }

        public static ChargeResult declined(String reason) {
            return new ChargeResult(false, null, reason);
        }
    }
}

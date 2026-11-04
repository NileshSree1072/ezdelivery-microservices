package com.ezdelivery.payment.service;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezdelivery.common.event.Events.StockReserved;
import com.ezdelivery.payment.gateway.PaymentGateway;
import com.ezdelivery.payment.gateway.PaymentGateway.ChargeResult;
import com.ezdelivery.payment.model.Payment;
import com.ezdelivery.payment.repository.PaymentRepository;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final PaymentGateway gateway;

    public PaymentService(PaymentRepository payments, PaymentGateway gateway) {
        this.payments = payments;
        this.gateway = gateway;
    }

    /**
     * Charges the order once. A redelivered event returns the stored payment instead of charging
     * again; the UNIQUE(order_id) constraint backs this up if two deliveries race.
     */
    @Transactional
    public Payment charge(StockReserved event) {
        Optional<Payment> existing = payments.findByOrderId(event.orderId());
        if (existing.isPresent()) {
            log.info("Order {} already has a {} payment, not charging again", event.orderId(), existing.get().getStatus());
            return existing.get();
        }

        ChargeResult result = gateway.charge(event.orderId(), event.totalAmount(), event.customerEmail());
        Payment payment = new Payment(event.orderId(), event.totalAmount(), gateway.name(),
                result.success() ? Payment.Status.SUCCEEDED : Payment.Status.FAILED,
                result.reference(), result.failureReason());
        log.info("Payment for order {} {}: {}", event.orderId(), payment.getStatus(),
                result.success() ? result.reference() : result.failureReason());
        return payments.save(payment);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByOrder(UUID orderId) {
        return payments.findByOrderId(orderId);
    }
}

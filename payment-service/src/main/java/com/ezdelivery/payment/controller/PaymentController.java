package com.ezdelivery.payment.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ezdelivery.payment.model.Payment;
import com.ezdelivery.payment.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> byOrder(@PathVariable UUID orderId) {
        return ResponseEntity.of(service.findByOrder(orderId).map(PaymentResponse::from));
    }

    public record PaymentResponse(UUID id, UUID orderId, BigDecimal amount, Payment.Status status,
                                  String provider, String failureReason, Instant createdAt) {
        static PaymentResponse from(Payment p) {
            return new PaymentResponse(p.getId(), p.getOrderId(), p.getAmount(), p.getStatus(),
                    p.getProvider(), p.getFailureReason(), p.getCreatedAt());
        }
    }
}

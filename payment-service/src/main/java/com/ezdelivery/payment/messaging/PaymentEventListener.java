package com.ezdelivery.payment.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ezdelivery.common.event.Events.PaymentFailed;
import com.ezdelivery.common.event.Events.PaymentSucceeded;
import com.ezdelivery.common.event.Events.StockReserved;
import com.ezdelivery.common.event.Topics;
import com.ezdelivery.payment.model.Payment;
import com.ezdelivery.payment.service.PaymentService;

@Component
public class PaymentEventListener {

    private final PaymentService payments;
    private final KafkaTemplate<String, Object> kafka;

    public PaymentEventListener(PaymentService payments, KafkaTemplate<String, Object> kafka) {
        this.payments = payments;
        this.kafka = kafka;
    }

    @KafkaListener(topics = Topics.STOCK_RESERVED)
    public void onStockReserved(StockReserved event) {
        Payment payment = payments.charge(event);
        String key = event.orderId().toString();
        if (payment.getStatus() == Payment.Status.SUCCEEDED) {
            kafka.send(Topics.PAYMENT_SUCCEEDED, key,
                    new PaymentSucceeded(event.orderId(), payment.getId(), payment.getAmount())).join();
        } else {
            kafka.send(Topics.PAYMENT_FAILED, key,
                    new PaymentFailed(event.orderId(), payment.getFailureReason())).join();
        }
    }
}

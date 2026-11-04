package com.ezdelivery.order.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezdelivery.common.event.Events.OrderCreated;
import com.ezdelivery.common.event.Events.OrderLineItem;
import com.ezdelivery.common.event.Topics;
import com.ezdelivery.order.client.ProductClient;
import com.ezdelivery.order.client.ProductClient.ProductSummary;
import com.ezdelivery.order.dto.OrderDtos.CreateOrderRequest;
import com.ezdelivery.order.exception.Exceptions.InvalidOrderException;
import com.ezdelivery.order.exception.Exceptions.OrderNotFoundException;
import com.ezdelivery.order.model.CustomerOrder;
import com.ezdelivery.order.model.OrderStatus;
import com.ezdelivery.order.repository.OrderRepository;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final ProductClient products;
    private final KafkaTemplate<String, Object> kafka;

    public OrderService(OrderRepository orders, ProductClient products, KafkaTemplate<String, Object> kafka) {
        this.orders = orders;
        this.products = products;
        this.kafka = kafka;
    }

    /**
     * Saves the order as PENDING, then publishes order.created to start the saga.
     * Returns immediately: the final status (CONFIRMED / CANCELLED) arrives asynchronously.
     */
    public CustomerOrder placeOrder(CreateOrderRequest request) {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        request.items().forEach(i -> quantities.merge(i.productId(), i.quantity(), Integer::sum));

        CustomerOrder order = new CustomerOrder(request.customerEmail(), request.customerName(),
                request.streetAddress(), request.city(), request.postalCode(), request.country());

        quantities.forEach((productId, qty) -> {
            ProductSummary product = products.findProduct(productId)
                    .orElseThrow(() -> new InvalidOrderException("Unknown product: " + productId));
            order.addLine(product.id(), product.title(), product.price(), qty);
        });

        CustomerOrder saved = orders.save(order);

        List<OrderLineItem> items = saved.getLines().stream()
                .map(l -> new OrderLineItem(l.getProductId(), l.getQuantity(), l.getUnitPrice()))
                .toList();
        kafka.send(Topics.ORDER_CREATED, saved.getId().toString(),
                new OrderCreated(saved.getId(), saved.getCustomerEmail(), items, saved.getTotalAmount())).join();
        log.info("Order {} placed: total {}, {} line(s)", saved.getId(), saved.getTotalAmount(), items.size());
        return saved;
    }

    /**
     * @return the order if it is now in {@code target}, including when a redelivered event finds it
     *         already there, so the caller can safely re-publish a follow-up event that failed before
     */
    @Transactional
    public Optional<CustomerOrder> transition(UUID orderId, OrderStatus target, String reason) {
        CustomerOrder order = orders.findByIdForUpdate(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        if (order.transitionTo(target, reason)) {
            log.info("Order {} -> {}{}", orderId, target, reason == null ? "" : " (" + reason + ")");
        } else if (order.getStatus() != target) {
            log.info("Order {} ignored {} (current status {})", orderId, target, order.getStatus());
            return Optional.empty();
        }
        return Optional.of(order);
    }

    @Transactional(readOnly = true)
    public CustomerOrder get(UUID id) {
        return orders.findWithLinesById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> findByCustomer(String email) {
        return orders.findByCustomerEmailOrderByCreatedAtDesc(email);
    }
}

package com.ezdelivery.order.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ezdelivery.order.dto.OrderDtos.CreateOrderRequest;
import com.ezdelivery.order.dto.OrderDtos.OrderResponse;
import com.ezdelivery.order.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    /** Returns 202 Accepted: the order is PENDING until the saga finishes. Poll GET /api/orders/{id}. */
    @PostMapping
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = OrderResponse.from(service.placeOrder(request));
        return ResponseEntity.accepted().location(URI.create("/api/orders/" + order.id())).body(order);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return OrderResponse.from(service.get(id));
    }

    @GetMapping
    public List<OrderResponse> byCustomer(@RequestParam String email) {
        return service.findByCustomer(email).stream().map(OrderResponse::from).toList();
    }
}

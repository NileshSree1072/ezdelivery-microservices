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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Orders", description = "Place and track orders")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    /** Returns 202 Accepted: the order is PENDING until the saga finishes. Poll GET /api/orders/{id}. */
    @Operation(summary = "Place an order",
            description = "Prices are looked up from the product service. Returns 202 with status PENDING; "
                    + "the saga then moves the order to CONFIRMED or CANCELLED within about a second.")
    @ApiResponse(responseCode = "202", description = "Order accepted, status PENDING")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "422", description = "Unknown product")
    @PostMapping
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = OrderResponse.from(service.placeOrder(request));
        return ResponseEntity.accepted().location(URI.create("/api/orders/" + order.id())).body(order);
    }

    @Operation(summary = "Get an order", description = "Includes the current saga status and failure reason, if any.")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return OrderResponse.from(service.get(id));
    }

    @Operation(summary = "List a customer's orders", description = "Newest first.")
    @GetMapping
    public List<OrderResponse> byCustomer(@RequestParam String email) {
        return service.findByCustomer(email).stream().map(OrderResponse::from).toList();
    }
}

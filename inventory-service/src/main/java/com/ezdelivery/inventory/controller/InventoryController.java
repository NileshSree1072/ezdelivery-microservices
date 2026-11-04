package com.ezdelivery.inventory.controller;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ezdelivery.inventory.model.InventoryItem;
import com.ezdelivery.inventory.service.InventoryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<StockResponse> get(@PathVariable String productId) {
        return ResponseEntity.of(service.find(productId).map(StockResponse::from));
    }

    @PutMapping("/{productId}")
    public StockResponse setStock(@PathVariable String productId, @Valid @RequestBody StockRequest request) {
        return StockResponse.from(service.setAvailable(productId, request.availableQuantity()));
    }

    public record StockRequest(@Min(0) @Max(1_000_000) int availableQuantity) {
    }

    public record StockResponse(String productId, int availableQuantity, int reservedQuantity, Instant updatedAt) {
        static StockResponse from(InventoryItem i) {
            return new StockResponse(i.getProductId(), i.getAvailableQuantity(), i.getReservedQuantity(), i.getUpdatedAt());
        }
    }
}

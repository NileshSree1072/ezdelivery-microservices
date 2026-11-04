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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Tag(name = "Inventory", description = "Stock levels per product")
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @Operation(summary = "Get stock for a product", description = "Available units and units held for unpaid orders.")
    @ApiResponse(responseCode = "404", description = "No stock record for this product")
    @GetMapping("/{productId}")
    public ResponseEntity<StockResponse> get(@PathVariable String productId) {
        return ResponseEntity.of(service.find(productId).map(StockResponse::from));
    }

    @Operation(summary = "Set available stock", description = "Creates the stock record if it does not exist.")
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

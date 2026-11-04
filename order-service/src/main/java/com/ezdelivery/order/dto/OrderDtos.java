package com.ezdelivery.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.ezdelivery.order.model.CustomerOrder;
import com.ezdelivery.order.model.OrderStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record CreateOrderRequest(
            @NotBlank @Email String customerEmail,
            @NotBlank @Size(max = 120) String customerName,
            @Size(max = 255) String streetAddress,
            @Size(max = 100) String city,
            @Size(max = 20) String postalCode,
            @Size(max = 100) String country,
            @NotEmpty @Size(max = 50) List<@Valid Item> items) {

        public record Item(@NotBlank String productId, @Min(1) @Max(100) int quantity) {
        }
    }

    public record OrderResponse(
            UUID id,
            OrderStatus status,
            String customerEmail,
            String customerName,
            BigDecimal totalAmount,
            String failureReason,
            List<Line> items,
            Instant createdAt,
            Instant updatedAt) {

        public record Line(String productId, String productTitle, BigDecimal unitPrice, int quantity) {
        }

        public static OrderResponse from(CustomerOrder o) {
            return new OrderResponse(o.getId(), o.getStatus(), o.getCustomerEmail(), o.getCustomerName(),
                    o.getTotalAmount(), o.getFailureReason(),
                    o.getLines().stream()
                            .map(l -> new Line(l.getProductId(), l.getProductTitle(), l.getUnitPrice(), l.getQuantity()))
                            .toList(),
                    o.getCreatedAt(), o.getUpdatedAt());
        }
    }
}

package com.ezdelivery.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.ezdelivery.product.model.Product;

public record ProductResponse(
        String id,
        String title,
        String description,
        BigDecimal price,
        List<String> images,
        String categoryId,
        Map<String, Object> properties,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getTitle(), p.getDescription(), p.getPrice(),
                p.getImages(), p.getCategoryId(), p.getProperties(), p.getCreatedAt(), p.getUpdatedAt());
    }
}

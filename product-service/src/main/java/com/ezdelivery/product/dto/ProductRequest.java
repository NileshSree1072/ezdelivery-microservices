package com.ezdelivery.product.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        List<String> images,
        String categoryId,
        Map<String, Object> properties) {
}

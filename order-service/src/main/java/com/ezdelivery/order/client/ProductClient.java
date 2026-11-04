package com.ezdelivery.order.client;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Synchronous lookup of the current price. The client never decides the price;
 * the order service asks the product service, the source of truth for the catalog.
 */
@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(RestClient.Builder builder, @Value("${services.product.url}") String productServiceUrl) {
        this.restClient = builder.baseUrl(productServiceUrl).build();
    }

    public Optional<ProductSummary> findProduct(String productId) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .body(ProductSummary.class));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public record ProductSummary(String id, String title, BigDecimal price) {
    }
}

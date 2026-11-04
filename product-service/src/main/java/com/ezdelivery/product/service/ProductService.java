package com.ezdelivery.product.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ezdelivery.product.dto.ProductRequest;
import com.ezdelivery.product.dto.ProductResponse;
import com.ezdelivery.product.exception.ProductNotFoundException;
import com.ezdelivery.product.model.Product;
import com.ezdelivery.product.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<ProductResponse> findAll(String categoryId) {
        List<Product> products = categoryId == null
                ? repository.findAll()
                : repository.findByCategoryId(categoryId);
        return products.stream().map(ProductResponse::from).toList();
    }

    public ProductResponse findById(String id) {
        return ProductResponse.from(getOrThrow(id));
    }

    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return ProductResponse.from(repository.save(product));
    }

    public ProductResponse update(String id, ProductRequest request) {
        Product product = getOrThrow(id);
        apply(product, request);
        return ProductResponse.from(repository.save(product));
    }

    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        repository.deleteById(id);
    }

    private Product getOrThrow(String id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    private static void apply(Product product, ProductRequest request) {
        product.setTitle(request.title());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setImages(request.images() == null ? new ArrayList<>() : request.images());
        product.setCategoryId(request.categoryId());
        product.setProperties(request.properties() == null ? new HashMap<>() : request.properties());
    }
}

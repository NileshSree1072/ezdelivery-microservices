package com.ezdelivery.product.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ezdelivery.product.model.Product;

public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findByCategoryId(String categoryId);
}

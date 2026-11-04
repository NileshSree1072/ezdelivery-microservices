package com.ezdelivery.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.ezdelivery.product.dto.ProductRequest;
import com.ezdelivery.product.dto.ProductResponse;
import com.ezdelivery.product.exception.ProductNotFoundException;
import com.ezdelivery.product.model.Product;
import com.ezdelivery.product.repository.ProductRepository;

@SpringBootTest
@Testcontainers
class ProductCacheIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired
    ProductService service;

    @Autowired
    ProductRepository repository;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    StringRedisTemplate redisTemplate;

    @BeforeEach
    void clean() {
        repository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    private ProductResponse create(String title, String price) {
        return service.create(new ProductRequest(title, null, new BigDecimal(price), null, null, null));
    }

    /** Changes the price behind the service's back, so only a cache hit returns the old value. */
    private void changePriceDirectlyInMongo(String id, String price) {
        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(id)),
                Update.update("price", new BigDecimal(price)), Product.class);
    }

    @Test
    void secondReadIsServedFromRedis() {
        ProductResponse p = create("Apples", "120.00");
        service.findById(p.id());

        changePriceDirectlyInMongo(p.id(), "1.00");

        assertThat(service.findById(p.id()).price()).isEqualByComparingTo("120.00");
        assertThat(redisTemplate.hasKey("ezdelivery:products::" + p.id())).isTrue();
    }

    @Test
    void updateRefreshesTheCachedProduct() {
        ProductResponse p = create("Apples", "120.00");
        service.findById(p.id());

        service.update(p.id(), new ProductRequest("Apples", null, new BigDecimal("99.00"), null, null, null));

        assertThat(service.findById(p.id()).price()).isEqualByComparingTo("99.00");
    }

    @Test
    void deleteEvictsTheCachedProduct() {
        ProductResponse p = create("Apples", "120.00");
        service.findById(p.id());

        service.delete(p.id());

        assertThatThrownBy(() -> service.findById(p.id())).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void createdProductAppearsInCachedList() {
        create("Apples", "120.00");
        assertThat(service.findAll(null)).hasSize(1);

        create("Pears", "150.00");

        assertThat(service.findAll(null)).extracting(ProductResponse::title).containsExactlyInAnyOrder("Apples", "Pears");
    }
}

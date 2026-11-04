package com.ezdelivery.product.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import com.ezdelivery.product.dto.ProductResponse;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Cache-aside with Redis: reads check Redis first and fall back to MongoDB on a miss.
 * Writes update or evict the affected entries so readers never see stale prices for long.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String PRODUCTS = "products";
    public static final String PRODUCT_LISTS = "productLists";

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCaches(
            ObjectMapper objectMapper,
            @Value("${cache.ttl.product:10m}") Duration productTtl,
            @Value("${cache.ttl.product-list:2m}") Duration listTtl) {

        JavaType productType = objectMapper.constructType(ProductResponse.class);
        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, ProductResponse.class);

        return builder -> builder.withInitialCacheConfigurations(Map.of(
                PRODUCTS, cacheConfig(objectMapper, productType, productTtl),
                PRODUCT_LISTS, cacheConfig(objectMapper, listType, listTtl)));
    }

    private static RedisCacheConfiguration cacheConfig(ObjectMapper mapper, JavaType type, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("ezdelivery:")
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(mapper, type)));
    }

    /** If Redis is down, log and fall through to MongoDB instead of failing the request. */
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }
}

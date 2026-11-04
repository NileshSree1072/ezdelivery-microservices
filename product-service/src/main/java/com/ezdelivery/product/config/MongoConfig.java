package com.ezdelivery.product.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/** Enables @CreatedDate / @LastModifiedDate, matching the timestamps in the original Mongoose schema. */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}

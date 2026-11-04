package com.ezdelivery.inventory.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    /**
     * The relative server URL makes Swagger UI's "Try it out" call whichever host served the page,
     * so requests go through the API gateway rather than to the service's internal address.
     */
    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory Service")
                        .version("0.1.0")
                        .description("Stock levels per product. Stock is reserved, released and committed by Kafka saga events."))
                .servers(List.of(new Server().url("/").description("API gateway")));
    }
}

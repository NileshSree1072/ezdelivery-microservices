package com.ezdelivery.gateway;

import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.netty.resolver.DefaultAddressResolverGroup;

@Configuration
public class HttpClientConfig {

    /**
     * Resolve service hostnames with the JVM resolver (TTL set by -Dsun.net.inetaddr.ttl) instead of
     * Netty's DNS cache, which keeps addresses for the full DNS TTL. Without this, a redeployed
     * service that comes back on a new container IP stays unreachable through the gateway.
     */
    @Bean
    public HttpClientCustomizer jvmDnsResolver() {
        return httpClient -> httpClient.resolver(DefaultAddressResolverGroup.INSTANCE);
    }
}

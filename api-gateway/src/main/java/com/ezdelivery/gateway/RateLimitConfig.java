package com.ezdelivery.gateway;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    /**
     * Rate-limits by the caller's IP address. Uses the TCP remote address rather than the
     * X-Forwarded-For header, which a client could fake to dodge the limit.
     */
    @Bean
    public KeyResolver clientIpKeyResolver() {
        return exchange -> Mono.just(Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getAddress)
                .map(InetAddress::getHostAddress)
                .orElse("unknown"));
    }
}

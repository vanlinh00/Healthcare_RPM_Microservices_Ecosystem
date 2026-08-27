package com.healthcare.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Configuration
public class RateLimiterConfig {

    /**
     * Rate limiter resolves user ID from JWT principal or falls back to client remote IP address.
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(principal -> principal.getName())
                .switchIfEmpty(Mono.justOrEmpty(
                        Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                                .map(addr -> addr.getAddress().getHostAddress())
                                .orElse("anonymous")
                ));
    }
}

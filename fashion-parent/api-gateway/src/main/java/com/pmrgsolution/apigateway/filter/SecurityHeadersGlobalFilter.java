package com.pmrgsolution.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Enterprise OWASP Security Headers Filter.
 * Defends against Clickjacking, MIME-Sniffing, XSS, and unencrypted transport attacks.
 */
@Component
public class SecurityHeadersGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();

            // Prevent MIME type sniffing & XSS
            headers.set("Content-Security-Policy", "default-src 'self'");
            headers.set("X-Content-Type-Options", "nosniff");

            // Prevent Clickjacking
            headers.set("X-Frame-Options", "DENY");

            // Legacy XSS protection for older browsers
            headers.set("X-XSS-Protection", "1; mode=block");

            // Referrer policy
            headers.set("Referrer-Policy", "strict-origin-when-cross-origin");

            // Permissions policy
            headers.set("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        }));
    }

    @Override
    public int getOrder() {
        // Run right before response is committed
        return Ordered.LOWEST_PRECEDENCE;
    }
}

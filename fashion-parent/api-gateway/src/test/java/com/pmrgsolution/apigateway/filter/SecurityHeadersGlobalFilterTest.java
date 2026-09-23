package com.pmrgsolution.apigateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityHeadersGlobalFilterTest {

    private SecurityHeadersGlobalFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SecurityHeadersGlobalFilter();
    }

    @Test
    @DisplayName("filter injects all 7 OWASP security headers on response")
    void filter_injectsSecurityHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mutatedExchange -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        HttpHeaders headers = exchange.getResponse().getHeaders();
        assertThat(headers.getFirst("Strict-Transport-Security")).contains("max-age=31536000");
        assertThat(headers.getFirst("Content-Security-Policy")).isEqualTo("default-src 'self'");
        assertThat(headers.getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(headers.getFirst("X-XSS-Protection")).isEqualTo("1; mode=block");
        assertThat(headers.getFirst("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
        assertThat(headers.getFirst("Permissions-Policy")).isEqualTo("geolocation=(), microphone=(), camera=()");
    }

    @Test
    @DisplayName("filter order is LOWEST_PRECEDENCE")
    void getOrder_returnsLowestPrecedence() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }
}

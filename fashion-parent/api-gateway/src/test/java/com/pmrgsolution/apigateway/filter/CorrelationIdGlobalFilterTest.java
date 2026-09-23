package com.pmrgsolution.apigateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdGlobalFilterTest {

    private CorrelationIdGlobalFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdGlobalFilter();
    }

    @Test
    @DisplayName("filter generates new correlation ID when missing in request")
    void filter_generatesCorrelationId_whenMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<String> injectedRequestCorrelationId = new AtomicReference<>();
        GatewayFilterChain chain = mutatedExchange -> {
            injectedRequestCorrelationId.set(mutatedExchange.getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(injectedRequestCorrelationId.get()).isNotNull().isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER))
                .isEqualTo(injectedRequestCorrelationId.get());
    }

    @Test
    @DisplayName("filter preserves existing correlation ID when present")
    void filter_preservesExistingCorrelationId() {
        String existingCorrelationId = "custom-correlation-id-12345";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products")
                .header(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER, existingCorrelationId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<String> injectedRequestCorrelationId = new AtomicReference<>();
        GatewayFilterChain chain = mutatedExchange -> {
            injectedRequestCorrelationId.set(mutatedExchange.getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(injectedRequestCorrelationId.get()).isEqualTo(existingCorrelationId);
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER))
                .isEqualTo(existingCorrelationId);
    }

    @Test
    @DisplayName("filter order is HIGHEST_PRECEDENCE")
    void getOrder_returnsHighestPrecedence() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}

package com.pmrgsolution.apigateway.filter;

import com.pmrgsolution.apigateway.config.JwtProperties;
import com.pmrgsolution.apigateway.security.JwtGatewayValidator;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtValidationGlobalFilter implements GlobalFilter, Ordered {

    private final JwtGatewayValidator jwtGatewayValidator;
    private final JwtProperties jwtProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> PUBLIC_PATH_PATTERNS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/resend-otp",
            "/api/v1/auth/google",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/email-service-status",
            "/api/v1/products/**",
            "/api/v1/categories/**",
            "/api/v1/banners/**",
            "/api/v1/contact/**",
            "/api/v1/store-info/**",
            "/api/v1/email/status",
            "/api/v1/payments/settings/public",
            "/actuator/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/api-docs/**",
            "/fallback/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        boolean isPublic = isPublicPath(path);
        String token = extractToken(request);

        if (token == null || token.isBlank()) {
            if (isPublic) {
                return chain.filter(exchange);
            }
            return writeUnauthorizedResponse(exchange, "Missing authentication token");
        }

        if (!jwtGatewayValidator.isTokenValid(token)) {
            if (isPublic) {
                return chain.filter(exchange);
            }
            return writeUnauthorizedResponse(exchange, "Invalid or expired authentication token");
        }

        Claims claims = jwtGatewayValidator.extractAllClaims(token);
        if (claims == null) {
            if (isPublic) {
                return chain.filter(exchange);
            }
            return writeUnauthorizedResponse(exchange, "Invalid token claims");
        }

        String jti = claims.getId();
        return jwtGatewayValidator.isBlacklisted(jti)
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        if (isPublic) {
                            return chain.filter(exchange);
                        }
                        log.warn("Blocked request with blacklisted token JTI: {}", jti);
                        return writeUnauthorizedResponse(exchange, "Token has been revoked");
                    }

                    String userId = claims.getSubject();
                    String email = claims.get("email", String.class);
                    List<String> roles = jwtGatewayValidator.extractRoles(claims);
                    String rolesStr = String.join(",", roles);

                    ServerHttpRequest.Builder requestBuilder = request.mutate();
                    if (userId != null) requestBuilder.header("X-User-Id", userId);
                    if (email != null) requestBuilder.header("X-User-Email", email);
                    if (!roles.isEmpty()) requestBuilder.header("X-User-Roles", rolesStr);

                    return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
                });
    }

    private boolean isPublicPath(String path) {
        for (String pattern : PUBLIC_PATH_PATTERNS) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        HttpCookie cookie = request.getCookies().getFirst(jwtProperties.getCookieName());
        if (cookie != null) {
            return cookie.getValue().trim();
        }

        return null;
    }

    private Mono<Void> writeUnauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"success\":false,\"status\":401,\"message\":\"%s\",\"data\":null,\"error\":{\"errorCode\":\"UNAUTHORIZED\",\"message\":\"%s\",\"timestamp\":\"%s\"},\"path\":\"%s\"}",
                message, message, Instant.now(), exchange.getRequest().getPath()
        );

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}

package com.pmrgsolution.apigateway.security;

import com.pmrgsolution.apigateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtGatewayValidator {

    private final JwtProperties jwtProperties;
    private final ReactiveStringRedisTemplate reactiveRedisTemplate;

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
            if (keyBytes.length < 64) {
                keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims != null && !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("Failed to extract claims from JWT: {}", e.getMessage());
            return null;
        }
    }

    public Mono<Boolean> isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return Mono.just(false);
        }
        return reactiveRedisTemplate.hasKey("blacklist:token:" + jti)
                .onErrorReturn(false);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        if (claims == null) return List.of();
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?> list) {
            return (List<String>) list;
        }
        String singleRole = claims.get("role", String.class);
        if (singleRole != null && !singleRole.isBlank()) {
            return List.of(singleRole);
        }
        return List.of();
    }
}

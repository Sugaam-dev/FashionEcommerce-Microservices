package com.pmrgsolution.apigateway.security;

import com.pmrgsolution.apigateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtGatewayValidatorTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    private JwtGatewayValidator validator;
    private final String secretKey = "dGhpcy1pcy1hLXZlcnktc2VjdXJlLWFuZC1sb25nLWtleS1mb3ItaHM1MTItYWxnb3JpdGhtLWluLXByb2R1Y3Rpb24tbXVzdC1iZS02NC1ieXRlcy1vci1tb3Jl";

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secretKey);
        validator = new JwtGatewayValidator(properties, redisTemplate);
    }

    private String generateTestToken(UUID userId, String email, List<String> roles, String jti, long expirationMs) {
        SecretKey key;
        try {
            byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(secretKey);
            if (keyBytes.length >= 64) {
                key = Keys.hmacShaKeyFor(keyBytes);
            } else {
                key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        }
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .id(jti)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("isTokenValid with valid unexpired token returns true")
    void isTokenValid_withValidToken_returnsTrue() {
        UUID userId = UUID.randomUUID();
        String token = generateTestToken(userId, "customer@example.com", List.of("ROLE_CUSTOMER"), "jti-1", 60000);

        assertThat(validator.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid with expired token returns false")
    void isTokenValid_withExpiredToken_returnsFalse() {
        UUID userId = UUID.randomUUID();
        String token = generateTestToken(userId, "customer@example.com", List.of("ROLE_CUSTOMER"), "jti-1", -1000);

        assertThat(validator.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("extractAllClaims extracts subject, email, roles, and JTI")
    void extractAllClaims_extractsClaimsCorrectly() {
        UUID userId = UUID.randomUUID();
        String token = generateTestToken(userId, "admin@example.com", List.of("ROLE_ADMIN", "ROLE_CUSTOMER"), "jti-admin-1", 60000);

        Claims claims = validator.extractAllClaims(token);

        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("admin@example.com");
        assertThat(claims.getId()).isEqualTo("jti-admin-1");

        List<String> roles = validator.extractRoles(claims);
        assertThat(roles).containsExactly("ROLE_ADMIN", "ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("isBlacklisted returns true when Redis contains blacklist key")
    void isBlacklisted_whenInRedis_returnsTrue() {
        when(redisTemplate.hasKey(eq("blacklist:token:revoked-jti"))).thenReturn(Mono.just(true));

        StepVerifier.create(validator.isBlacklisted("revoked-jti"))
                .expectNext(true)
                .verifyComplete();
    }
}

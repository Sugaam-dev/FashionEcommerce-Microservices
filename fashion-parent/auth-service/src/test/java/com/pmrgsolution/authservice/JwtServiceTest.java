package com.pmrgsolution.authservice;

import com.pmrgsolution.authservice.config.JwtProperties;
import com.pmrgsolution.authservice.enums.AccountStatus;
import com.pmrgsolution.authservice.enums.Role;
import com.pmrgsolution.authservice.security.JwtService;
import com.pmrgsolution.authservice.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecretKey("AStrongHS512SecretKeyForTestingPurposeThatIsAtLeastSixtyFourBytesLongStandard2026!");
        jwtProperties.setAccessExpirationMs(900000L);
        jwtProperties.setRefreshExpirationMs(604800000L);
        jwtProperties.setIssuer("fashion-ecommerce-auth");
        jwtProperties.setAudience("ecommerce-api");

        jwtService = new JwtService(jwtProperties);
        jwtService.init();
    }

    @Test
    void testGenerateAndValidateAccessToken() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.builder()
                .id(userId)
                .email("test@example.com")
                .password("encoded_pass")
                .role(Role.ROLE_CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .emailVerified(true)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .build();

        String token = jwtService.generateAccessToken(principal);
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals("test@example.com", jwtService.extractEmail(token));
        assertNotNull(jwtService.extractJti(token));
    }

    @Test
    void testTokenHashing() {
        String token = "sample-refresh-token-value-12345";
        String hash1 = jwtService.hashToken(token);
        String hash2 = jwtService.hashToken(token);

        assertNotNull(hash1);
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length()); // SHA-256 hex length
    }
}

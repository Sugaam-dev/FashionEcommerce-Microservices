package com.pmrgsolution.authservice.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Validated
@Getter
@Setter
public class JwtProperties {

    @NotBlank(message = "JWT Secret key is required and must be at least 512 bits for HS512")
    private String secretKey;

    @NotNull(message = "Access token expiration time is required")
    private Long accessExpirationMs = 900000L; // 15 minutes

    @NotNull(message = "Refresh token expiration time is required")
    private Long refreshExpirationMs = 604800000L; // 7 days

    @NotBlank(message = "JWT Issuer is required")
    private String issuer = "fashion-ecommerce-auth";

    @NotBlank(message = "JWT Audience is required")
    private String audience = "ecommerce-api";

    private String cookieAccessName = "accessToken";

    private String cookieRefreshName = "refreshToken";

    private boolean cookieSecure = false;

    private String cookieSameSite = "Lax";

    private String cookieDomain;

    private String internalServiceKey;

    private String googleClientId;
}

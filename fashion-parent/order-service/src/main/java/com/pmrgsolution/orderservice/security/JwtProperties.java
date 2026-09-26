package com.pmrgsolution.orderservice.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    @NotBlank(message = "JWT secret key is required")
    private String secretKey;

    private String cookieAccessName = "accessToken";
}

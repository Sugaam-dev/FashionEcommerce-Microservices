package com.pmrgsolution.notificationservice.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secretKey = "dGhpcy1pcy1hLXZlcnktc2VjdXJlLWFuZC1sb25nLWtleS1mb3ItaHM1MTItYWxnb3JpdGhtLWluLXByb2R1Y3Rpb24tbXVzdC1iZS02NC1ieXRlcy1vci1tb3Jl";
    private String issuer = "com.pmrgsolution";
    private String audience = "ecommerce-api";
    private String cookieName = "app-jwt";
}

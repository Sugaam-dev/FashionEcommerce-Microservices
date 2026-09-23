package com.pmrgsolution.apigateway;

import com.pmrgsolution.shared.core.config.CommonCoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(
        scanBasePackages = "com.pmrgsolution.apigateway",
        exclude = {
                ReactiveUserDetailsServiceAutoConfiguration.class,
                ReactiveSecurityAutoConfiguration.class,
                CommonCoreAutoConfiguration.class
        }
)
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

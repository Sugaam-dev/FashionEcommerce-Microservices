package com.pmrgsolution.authservice.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.pmrgsolution.authservice")
public class FeignClientConfig {
}

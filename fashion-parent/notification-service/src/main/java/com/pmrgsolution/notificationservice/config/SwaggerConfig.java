package com.pmrgsolution.notificationservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FashionEcommerce - Notification Service API")
                        .description("REST & Event-driven notification dispatch, email delivery, SSE real-time alerts, and store info management.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PMRG Solutions")
                                .email("support@pmrgsolution.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")));
    }
}

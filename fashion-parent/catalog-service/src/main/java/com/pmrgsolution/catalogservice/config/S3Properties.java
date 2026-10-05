package com.pmrgsolution.catalogservice.config;

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
@ConfigurationProperties(prefix = "app.s3")
public class S3Properties {

    @NotBlank(message = "S3 endpoint is required")
    private String endpoint = "http://localhost:4566";

    @NotBlank(message = "S3 region is required")
    private String region = "ap-south-1";

    @NotBlank(message = "S3 bucket name is required")
    private String bucketName = "fashion-ecommerce-media";

    @NotBlank(message = "S3 access key is required")
    private String accessKey = "test";

    @NotBlank(message = "S3 secret key is required")
    private String secretKey = "test";

    private String publicBaseUrl;
}

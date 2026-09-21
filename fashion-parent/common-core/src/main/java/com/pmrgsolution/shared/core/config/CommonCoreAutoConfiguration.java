package com.pmrgsolution.shared.core.config;

import com.pmrgsolution.shared.core.exception.GlobalExceptionHandler;
import com.pmrgsolution.shared.core.filter.ApiLoggingFilter;
import com.pmrgsolution.shared.core.filter.CorrelationIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

/**
 * Enterprise Spring Boot 3 Auto-Configuration for shared-core.
 * Automatically provisions platform exception handling and HTTP observability filters
 * to any servlet-based microservice without requiring manual component scanning.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({
        CorrelationIdFilter.class,
        ApiLoggingFilter.class,
        GlobalExceptionHandler.class
})
public class CommonCoreAutoConfiguration {
}

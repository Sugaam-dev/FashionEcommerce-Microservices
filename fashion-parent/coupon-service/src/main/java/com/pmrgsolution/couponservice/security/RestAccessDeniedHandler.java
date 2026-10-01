package com.pmrgsolution.couponservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrgsolution.shared.core.exception.ErrorCode;
import com.pmrgsolution.shared.core.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        log.warn("Access denied to {}: {}", request.getRequestURI(), accessDeniedException.getMessage());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> apiResponse = ApiResponse.failure(
                HttpStatus.FORBIDDEN.value(),
                "You do not have permission to access this resource",
                new com.pmrgsolution.shared.core.response.ErrorResponse(
                        ErrorCode.FORBIDDEN,
                        "You do not have permission to access this resource",
                        null,
                        java.time.Instant.now()
                ),
                request.getRequestURI()
        );
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}

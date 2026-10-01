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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        log.warn("Unauthorized access attempt to {}: {}", request.getRequestURI(), authException.getMessage());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> apiResponse = ApiResponse.failure(
                HttpStatus.UNAUTHORIZED.value(),
                "Authentication required to access this resource",
                new com.pmrgsolution.shared.core.response.ErrorResponse(
                        ErrorCode.UNAUTHORIZED,
                        "Authentication required to access this resource",
                        null,
                        java.time.Instant.now()
                ),
                request.getRequestURI()
        );
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}

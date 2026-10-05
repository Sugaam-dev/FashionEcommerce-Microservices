package com.pmrgsolution.authservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrgsolution.shared.core.exception.ErrorCode;
import com.pmrgsolution.shared.core.response.ApiResponse;
import com.pmrgsolution.shared.core.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        log.warn("Unauthorized access attempt to URI: {} - Error: {}", request.getRequestURI(), authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ErrorResponse error = new ErrorResponse(
                ErrorCode.UNAUTHORIZED,
                authException.getMessage(),
                java.util.List.of(),
                java.time.Instant.now()
        );
        ApiResponse<Void> apiResponse = ApiResponse.failure(
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized",
                error,
                request.getRequestURI()
        );

        objectMapper.writeValue(response.getOutputStream(), apiResponse);
    }
}

package com.pmrgsolution.orderservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrgsolution.shared.core.exception.ErrorCode;
import com.pmrgsolution.shared.core.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class InternalAuthFilter extends OncePerRequestFilter {

    @Value("${app.internal.service-key:${app.internal-service-key:${app.security.internal-service-key:fashion-internal-microservice-secret-key-2026}}}")
    private String internalServiceKey;

    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/internal") && !path.startsWith("/internal");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String requestKey = request.getHeader("X-Internal-Service-Key");

        if (!StringUtils.hasText(requestKey) || !requestKey.equals(internalServiceKey)) {
            log.warn("Unauthorized internal service request to path: {}", request.getRequestURI());
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiResponse<Void> apiResponse = ApiResponse.failure(
                    HttpStatus.FORBIDDEN.value(),
                    "Access denied: Invalid or missing internal service key",
                    new com.pmrgsolution.shared.core.response.ErrorResponse(
                            ErrorCode.FORBIDDEN,
                            "Access denied: Invalid or missing internal service key",
                            null,
                            java.time.Instant.now()
                    ),
                    request.getRequestURI()
            );
            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "INTERNAL_SERVICE",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}

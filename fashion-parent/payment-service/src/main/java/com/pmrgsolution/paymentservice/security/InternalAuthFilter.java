package com.pmrgsolution.paymentservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class InternalAuthFilter extends OncePerRequestFilter {

    @Value("${app.internal.service-key:pmrg-internal-secret-key-change-in-production}")
    private String internalServiceKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/internal/")) {
            String incomingKey = request.getHeader("X-Internal-Service-Key");
            if (incomingKey == null || !incomingKey.equals(internalServiceKey)) {
                log.warn("Unauthorized internal service access attempt on: {}", path);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"status\":403,\"message\":\"Forbidden: Invalid internal service key\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

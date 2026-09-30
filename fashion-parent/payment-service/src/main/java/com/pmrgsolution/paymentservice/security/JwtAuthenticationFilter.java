package com.pmrgsolution.paymentservice.security;

import com.pmrgsolution.paymentservice.service.DistributedLockService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final DistributedLockService distributedLockService;

    @Value("${app.internal.service-key:${app.internal-service-key:pmrg-internal-secret-key-change-in-production}}")
    private String internalServiceKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Check Gateway pre-injected headers only if accompanied by valid X-Internal-Service-Key
        String headerUserId = request.getHeader("X-User-Id");
        String headerEmail = request.getHeader("X-User-Email");
        String headerRoles = request.getHeader("X-User-Roles");
        String incomingInternalKey = request.getHeader("X-Internal-Service-Key");

        if (headerUserId != null && !headerUserId.isBlank()) {
            if (incomingInternalKey != null && incomingInternalKey.equals(internalServiceKey)) {
                try {
                    UUID userId = UUID.fromString(headerUserId);
                    List<String> roles = headerRoles != null && !headerRoles.isBlank()
                            ? Arrays.stream(headerRoles.split(",")).map(String::trim).toList()
                            : List.of("ROLE_CUSTOMER");

                    UserPrincipal principal = UserPrincipal.builder()
                            .id(userId)
                            .email(headerEmail)
                            .roles(roles)
                            .build();

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    filterChain.doFilter(request, response);
                    return;
                } catch (Exception e) {
                    log.warn("Failed to construct principal from X-User headers: {}", e.getMessage());
                }
            } else {
                log.warn("Rejected untrusted X-User-* headers: missing or invalid X-Internal-Service-Key from IP {}", request.getRemoteAddr());
            }
        }

        // 2. Extract Token from Bearer header or Cookie
        String token = extractToken(request);
        if (token != null && jwtService.isTokenValid(token)) {
            String jti = jwtService.extractJti(token);
            if (jti != null && distributedLockService.hasKey("blacklist:token:" + jti)) {
                log.warn("Rejected blacklisted JWT token: {}", jti);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            UUID userId = jwtService.extractUserId(token);
            String email = jwtService.extractEmail(token);
            List<String> roles = jwtService.extractRoles(token);

            UserPrincipal principal = UserPrincipal.builder()
                    .id(userId)
                    .email(email)
                    .roles(roles)
                    .build();

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities()
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (jwtProperties.getCookieName().equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}

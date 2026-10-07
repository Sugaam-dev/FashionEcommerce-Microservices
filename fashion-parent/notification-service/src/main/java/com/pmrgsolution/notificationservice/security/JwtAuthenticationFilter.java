package com.pmrgsolution.notificationservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Value("${app.internal.service-key:${app.internal-service-key:pmrg-internal-secret-key-change-in-production}}")
    private String internalServiceKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Check Gateway pre-injected headers only if accompanied by valid internal service key
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

                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    UserPrincipal principal = UserPrincipal.builder()
                            .id(userId)
                            .email(headerEmail)
                            .role(authorities.get(0).getAuthority())
                            .authorities(authorities)
                            .build();

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            principal, null, authorities
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    filterChain.doFilter(request, response);
                    return;
                } catch (Exception e) {
                    log.warn("Failed to construct principal from X-User headers: {}", e.getMessage());
                }
            }
        }

        // 2. Validate Bearer token or Cookie
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && jwtService.isTokenValid(token)) {
            try {
                UUID userId = jwtService.extractUserId(token);
                String email = jwtService.extractEmail(token);
                List<String> roles = jwtService.extractRoles(token);

                List<SimpleGrantedAuthority> authorities;
                if (roles != null && !roles.isEmpty()) {
                    authorities = roles.stream()
                            .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                } else {
                    authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
                }

                String primaryRole = authorities.get(0).getAuthority();

                UserPrincipal principal = UserPrincipal.builder()
                        .id(userId)
                        .email(email)
                        .role(primaryRole)
                        .authorities(authorities)
                        .build();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                authorities
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                log.debug("Could not authenticate user from JWT: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
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

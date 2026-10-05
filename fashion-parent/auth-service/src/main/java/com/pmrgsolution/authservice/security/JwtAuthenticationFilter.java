package com.pmrgsolution.authservice.security;

import com.pmrgsolution.authservice.config.JwtProperties;
import com.pmrgsolution.shared.core.constants.CacheKeys;
import com.pmrgsolution.shared.core.constants.SecurityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProperties jwtProperties;

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/resend-otp",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/set-password",
            "/api/v1/internal",
            "/actuator",
            "/v3/api-docs",
            "/swagger-ui",
            "/favicon.ico",
            "/error"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Endpoints that require user authentication even under /api/v1/auth
        if (path.startsWith("/api/v1/auth/logout") || path.startsWith("/api/v1/auth/google/complete-profile")) {
            return false;
        }

        // Public Google OAuth initiation
        if (path.equals("/api/v1/auth/google") || path.startsWith("/api/v1/auth/google/")) {
            return true;
        }

        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String token = resolveToken(request);

            if (StringUtils.hasText(token) && jwtService.isTokenValid(token)) {
                String jti = jwtService.extractJti(token);

                if (isBlacklisted(jti)) {
                    log.warn("Rejected blacklisted JWT token with jti: {}", jti);
                    filterChain.doFilter(request, response);
                    return;
                }

                try {
                    UUID userId = jwtService.extractUserId(token);
                    UserDetails userDetails = userDetailsService.loadUserById(userId);

                    if (userDetails.isEnabled() && userDetails.isAccountNonLocked()) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (Exception e) {
                    log.error("Cannot set user authentication: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error during JWT authentication filter processing: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return bearerToken.substring(SecurityConstants.BEARER_PREFIX.length()).trim();
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (jwtProperties.getCookieAccessName().equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    private boolean isBlacklisted(String jti) {
        if (!StringUtils.hasText(jti)) {
            return false;
        }
        try {
            Boolean hasKey = stringRedisTemplate.hasKey(CacheKeys.BLACKLIST_TOKEN + jti);
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception e) {
            log.error("Redis connection unavailable during JWT blacklist check for jti {}: {}. Allowing cryptographically verified token.",
                    jti, e.getMessage());
            return false;
        }
    }
}

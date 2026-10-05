package com.pmrgsolution.authservice.security;

import com.pmrgsolution.authservice.config.JwtProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InternalAuthFilter extends OncePerRequestFilter {

    public static final String INTERNAL_SERVICE_HEADER = "X-Internal-Service-Key";

    private final JwtProperties jwtProperties;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/internal")) {
            String providedKey = request.getHeader(INTERNAL_SERVICE_HEADER);

            if (StringUtils.hasText(providedKey) && providedKey.equals(jwtProperties.getInternalServiceKey())) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "internal-service",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                log.warn("Unauthorized access attempt to internal endpoint: {}", path);
            }
        }

        filterChain.doFilter(request, response);
    }
}

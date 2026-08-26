package com.pmrgsolution.shared.core.constants;

public final class SecurityConstants {
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    // Distributed Tracing & Correlation
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    // Downstream User Identity Headers (Propagated by Gateway)
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_EMAIL_HEADER = "X-User-Email";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    private SecurityConstants() {}
}

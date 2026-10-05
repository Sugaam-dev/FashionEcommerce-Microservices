package com.pmrgsolution.authservice.controller;

import com.pmrgsolution.authservice.config.JwtProperties;
import com.pmrgsolution.authservice.dto.request.CompleteGoogleProfileRequest;
import com.pmrgsolution.authservice.dto.request.ForgotPasswordRequest;
import com.pmrgsolution.authservice.dto.request.GoogleLoginRequest;
import com.pmrgsolution.authservice.dto.request.LoginRequest;
import com.pmrgsolution.authservice.dto.request.RefreshTokenRequest;
import com.pmrgsolution.authservice.dto.request.RegisterRequest;
import com.pmrgsolution.authservice.dto.request.ResendOtpRequest;
import com.pmrgsolution.authservice.dto.request.ResetPasswordRequest;
import com.pmrgsolution.authservice.dto.request.SetPasswordRequest;
import com.pmrgsolution.authservice.dto.request.VerifyOtpRequest;
import com.pmrgsolution.authservice.dto.response.AuthResponse;
import com.pmrgsolution.authservice.dto.response.PublicEmailStatusResponse;
import com.pmrgsolution.authservice.dto.response.TokenRefreshResponse;
import com.pmrgsolution.authservice.security.UserPrincipal;
import com.pmrgsolution.authservice.service.AuthService;
import com.pmrgsolution.shared.core.constants.SecurityConstants;
import com.pmrgsolution.shared.core.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, authentication, tokens, OTP and password management")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account", description = "Creates a new user record in pending state and sends an OTP to the email")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);

        AuthResponse response = authService.register(request, ipAddress, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Registration initiated. Verification OTP sent to your email.", response));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify registration or action OTP", description = "Verifies the 6-digit OTP code and activates the account")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String ipAddress = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);

        AuthResponse response = authService.verifyOtp(request, ipAddress, userAgent);
        if (response.refreshToken() != null) {
            setAuthCookies(httpResponse, response.accessToken(), response.refreshToken());
        }

        return ResponseEntity.ok(ApiResponse.ok("OTP verified successfully.", response));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend verification OTP", description = "Generates and dispatches a new OTP to the user's email")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.ok("Verification OTP resent successfully.", null));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user with email and password", description = "Validates credentials, establishes a user session, and issues JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String ipAddress = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);

        AuthResponse response = authService.login(request, ipAddress, userAgent);
        setAuthCookies(httpResponse, response.accessToken(), response.refreshToken());

        return ResponseEntity.ok(ApiResponse.ok("Login successful.", response));
    }

    @PostMapping("/google")
    @Operation(summary = "Authenticate user with Google OAuth2 ID Token", description = "Verifies Google ID token, signs in or registers the user, and issues JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String ipAddress = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);

        AuthResponse response = authService.googleLogin(request, ipAddress, userAgent);
        setAuthCookies(httpResponse, response.accessToken(), response.refreshToken());

        return ResponseEntity.ok(ApiResponse.ok("Google authentication successful.", response));
    }

    @PostMapping("/google/complete-profile")
    @Operation(summary = "Complete profile for Google-registered user", description = "Adds first name, last name, and phone number for Google authenticated users")
    public ResponseEntity<ApiResponse<AuthResponse>> completeGoogleProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CompleteGoogleProfileRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String ipAddress = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);

        AuthResponse response = authService.completeGoogleProfile(principal.getId(), request, ipAddress, userAgent);
        setAuthCookies(httpResponse, response.accessToken(), response.refreshToken());

        return ResponseEntity.ok(ApiResponse.ok("Profile completed successfully.", response));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Rotate refresh token and issue a new access token", description = "Uses an active refresh token to obtain a fresh access token pair")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String token = (requestBody != null && StringUtils.hasText(requestBody.refreshToken()))
                ? requestBody.refreshToken()
                : cookieRefreshToken;

        if (!StringUtils.hasText(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure(401, "Refresh token is missing", null, httpRequest.getRequestURI()));
        }

        String ipAddress = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);

        TokenRefreshResponse response = authService.refreshToken(new RefreshTokenRequest(token), ipAddress, userAgent);
        setAuthCookies(httpResponse, response.accessToken(), response.refreshToken());

        return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully.", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out user and invalidate session", description = "Revokes the active refresh token and blacklists the access token in Redis")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String authHeader = httpRequest.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        String accessToken = (StringUtils.hasText(authHeader) && authHeader.startsWith(SecurityConstants.BEARER_PREFIX))
                ? authHeader.substring(SecurityConstants.BEARER_PREFIX.length()).trim()
                : null;

        authService.logout(accessToken, cookieRefreshToken, principal != null ? principal.getId() : null);
        clearAuthCookies(httpResponse);

        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully.", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Initiate password reset flow", description = "Generates and sends a 6-digit password reset OTP to the user's email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("If an account with that email exists, a password reset OTP has been sent.", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Complete password reset with OTP", description = "Verifies the reset OTP and sets a new password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully. Please login with your new password.", null));
    }

    @PostMapping("/set-password")
    @Operation(summary = "Set or update account password", description = "Sets password for logged-in user")
    public ResponseEntity<ApiResponse<Void>> setPassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SetPasswordRequest request
    ) {
        authService.setPassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password updated successfully.", null));
    }

    @GetMapping("/email-status")
    @Operation(summary = "Check public email delivery service status", description = "Returns operational health for notification dispatch")
    public ResponseEntity<ApiResponse<PublicEmailStatusResponse>> checkEmailStatus(@RequestParam("email") String email) {
        PublicEmailStatusResponse response = authService.checkEmailStatus(email);
        return ResponseEntity.ok(ApiResponse.ok("Email status retrieved.", response));
    }

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        ResponseCookie accessCookie = ResponseCookie.from(jwtProperties.getCookieAccessName(), accessToken)
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .path("/")
                .maxAge(jwtProperties.getAccessExpirationMs() / 1000)
                .sameSite(jwtProperties.getCookieSameSite())
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from(jwtProperties.getCookieRefreshName(), refreshToken)
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .path("/")
                .maxAge(jwtProperties.getRefreshExpirationMs() / 1000)
                .sameSite(jwtProperties.getCookieSameSite())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from(jwtProperties.getCookieAccessName(), "")
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .path("/")
                .maxAge(0)
                .sameSite(jwtProperties.getCookieSameSite())
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from(jwtProperties.getCookieRefreshName(), "")
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .path("/")
                .maxAge(0)
                .sameSite(jwtProperties.getCookieSameSite())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

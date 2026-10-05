package com.pmrgsolution.authservice.service;

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

import java.util.UUID;

public interface AuthService {

    AuthResponse register(RegisterRequest request, String ipAddress, String userAgent);

    AuthResponse verifyOtp(VerifyOtpRequest request, String ipAddress, String userAgent);

    void resendOtp(ResendOtpRequest request);

    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);

    AuthResponse googleLogin(GoogleLoginRequest request, String ipAddress, String userAgent);

    AuthResponse completeGoogleProfile(UUID userId, CompleteGoogleProfileRequest request, String ipAddress, String userAgent);

    TokenRefreshResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent);

    void logout(String accessToken, String refreshToken, UUID userId);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void setPassword(UUID userId, SetPasswordRequest request);

    PublicEmailStatusResponse checkEmailStatus(String email);
}

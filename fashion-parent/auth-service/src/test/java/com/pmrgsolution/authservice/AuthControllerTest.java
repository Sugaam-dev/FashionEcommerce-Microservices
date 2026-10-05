package com.pmrgsolution.authservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pmrgsolution.authservice.config.JwtProperties;
import com.pmrgsolution.authservice.controller.AuthController;
import com.pmrgsolution.authservice.dto.request.LoginRequest;
import com.pmrgsolution.authservice.dto.request.RegisterRequest;
import com.pmrgsolution.authservice.dto.response.AuthResponse;
import com.pmrgsolution.authservice.dto.response.UserSummaryResponse;
import com.pmrgsolution.authservice.enums.AccountStatus;
import com.pmrgsolution.authservice.enums.Role;
import com.pmrgsolution.authservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register: Returns 201 CREATED")
    void testRegister_Endpoint() throws Exception {
        RegisterRequest request = new RegisterRequest("john@example.com", "Password@123", "John", "Doe", "9876543210");
        UserSummaryResponse userSummary = new UserSummaryResponse(
                UUID.randomUUID(), "john@example.com", "John", "Doe", "9876543210",
                null, Role.ROLE_CUSTOMER, AccountStatus.PENDING_VERIFICATION, false, Instant.now()
        );
        AuthResponse response = new AuthResponse(null, null, 0L, userSummary);

        when(authService.register(any(RegisterRequest.class), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value("john@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login: Returns 200 OK and Set-Cookie headers")
    void testLogin_Endpoint() throws Exception {
        LoginRequest request = new LoginRequest("john@example.com", "Password@123");
        UserSummaryResponse userSummary = new UserSummaryResponse(
                UUID.randomUUID(), "john@example.com", "John", "Doe", "9876543210",
                null, Role.ROLE_CUSTOMER, AccountStatus.ACTIVE, true, Instant.now()
        );
        AuthResponse response = new AuthResponse("access.jwt.token", "refresh.jwt.token", 900L, userSummary);

        when(jwtProperties.getCookieAccessName()).thenReturn("accessToken");
        when(jwtProperties.getCookieRefreshName()).thenReturn("refreshToken");
        when(jwtProperties.getAccessExpirationMs()).thenReturn(900000L);
        when(jwtProperties.getRefreshExpirationMs()).thenReturn(604800000L);
        when(jwtProperties.getCookieSameSite()).thenReturn("Lax");
        when(authService.login(any(LoginRequest.class), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access.jwt.token"))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/email-status: Returns 200 OK")
    void testCheckEmailStatus_Endpoint() throws Exception {
        mockMvc.perform(get("/api/v1/auth/email-status")
                        .param("email", "john@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

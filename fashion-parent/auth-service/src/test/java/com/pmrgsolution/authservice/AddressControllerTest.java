package com.pmrgsolution.authservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pmrgsolution.authservice.controller.AddressController;
import com.pmrgsolution.authservice.dto.request.AddressRequest;
import com.pmrgsolution.authservice.dto.response.AddressResponse;
import com.pmrgsolution.authservice.enums.AccountStatus;
import com.pmrgsolution.authservice.enums.AddressType;
import com.pmrgsolution.authservice.enums.Role;
import com.pmrgsolution.authservice.security.UserPrincipal;
import com.pmrgsolution.authservice.service.AddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AddressService addressService;

    @InjectMocks
    private AddressController addressController;

    private UserPrincipal testPrincipal;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testPrincipal = UserPrincipal.builder()
                .id(userId)
                .email("john@example.com")
                .password("hash")
                .role(Role.ROLE_CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .emailVerified(true)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Custom argument resolver for @AuthenticationPrincipal in standalone MockMvc
        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return testPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(addressController)
                .setCustomArgumentResolvers(principalResolver)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/account/addresses: Returns list of addresses")
    void testGetAddresses() throws Exception {
        AddressResponse address = new AddressResponse(
                UUID.randomUUID(), AddressType.SHIPPING, "John Doe", "9876543210",
                "123 Main St", null, null, "Bangalore", "Karnataka", "560001", "IN", true, Instant.now()
        );

        when(addressService.getAddresses(userId)).thenReturn(List.of(address));

        mockMvc.perform(get("/api/v1/account/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].recipientName").value("John Doe"));
    }

    @Test
    @DisplayName("POST /api/v1/account/addresses: Returns 201 CREATED")
    void testCreateAddress() throws Exception {
        AddressRequest request = new AddressRequest(
                AddressType.SHIPPING, "John Doe", "9876543210",
                "123 Main St", null, null, "Bangalore", "Karnataka", "560001", "IN", true
        );
        AddressResponse address = new AddressResponse(
                UUID.randomUUID(), AddressType.SHIPPING, "John Doe", "9876543210",
                "123 Main St", null, null, "Bangalore", "Karnataka", "560001", "IN", true, Instant.now()
        );

        when(addressService.createAddress(eq(userId), any(AddressRequest.class))).thenReturn(address);

        mockMvc.perform(post("/api/v1/account/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recipientPhone").value("9876543210"));
    }

    @Test
    @DisplayName("DELETE /api/v1/account/addresses/{addressId}: Returns 200 OK")
    void testDeleteAddress() throws Exception {
        UUID addressId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/account/addresses/" + addressId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

package com.pmrgsolution.notificationservice;

import com.pmrgsolution.notificationservice.controller.StoreInfoController;
import com.pmrgsolution.notificationservice.dto.response.StoreInfoSettingsResponse;
import com.pmrgsolution.notificationservice.service.StoreInfoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StoreInfoController.class)
@AutoConfigureMockMvc(addFilters = false)
class StoreInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreInfoService storeInfoService;

    @Test
    @DisplayName("GET /api/v1/store-info should return store display info")
    void getStoreInfo_ShouldReturn200() throws Exception {
        StoreInfoSettingsResponse response = new StoreInfoSettingsResponse(
                UUID.randomUUID(),
                "Summer Sale Active!",
                "/sale",
                true,
                "[]",
                "7-day returns",
                "3 days standard delivery",
                "123 Fashion Blvd",
                "9 AM - 6 PM",
                "support@fashionecommerce.com",
                "+919876543210",
                Instant.now()
        );

        when(storeInfoService.getStoreInfo()).thenReturn(response);

        mockMvc.perform(get("/api/v1/store-info")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.announcementText").value("Summer Sale Active!"))
                .andExpect(jsonPath("$.data.supportEmail").value("support@fashionecommerce.com"));
    }
}

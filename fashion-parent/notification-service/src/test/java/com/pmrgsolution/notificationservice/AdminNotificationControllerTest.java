package com.pmrgsolution.notificationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrgsolution.notificationservice.controller.AdminNotificationController;
import com.pmrgsolution.notificationservice.dto.request.AdminReplyRequest;
import com.pmrgsolution.notificationservice.dto.response.ContactMessageResponse;
import com.pmrgsolution.notificationservice.dto.response.StoreInfoSettingsResponse;
import com.pmrgsolution.notificationservice.enums.ContactMessageStatus;
import com.pmrgsolution.notificationservice.repository.EmailUsageQuotaRepository;
import com.pmrgsolution.notificationservice.repository.NotificationLogRepository;
import com.pmrgsolution.notificationservice.service.ContactMessageService;
import com.pmrgsolution.notificationservice.service.StoreInfoService;
import com.pmrgsolution.shared.core.response.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminNotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationLogRepository notificationLogRepository;

    @MockitoBean
    private ContactMessageService contactMessageService;

    @MockitoBean
    private EmailUsageQuotaRepository emailUsageQuotaRepository;

    @MockitoBean
    private StoreInfoService storeInfoService;

    @Test
    @DisplayName("GET /api/v1/admin/notifications/logs should return paginated logs")
    void getLogs_ShouldReturn200() throws Exception {
        when(notificationLogRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/admin/notifications/logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/admin/notifications/contact-messages should return paginated messages")
    void getContactMessages_ShouldReturn200() throws Exception {
        PageResponse<ContactMessageResponse> pageResponse = PageResponse.from(new PageImpl<>(List.of()));
        when(contactMessageService.getMessages(any(), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/notifications/contact-messages")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/notifications/contact-messages/{id}/status should update message")
    void updateContactMessageStatus_ShouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        AdminReplyRequest request = new AdminReplyRequest(ContactMessageStatus.REPLIED, "We are resolving your issue.");

        ContactMessageResponse response = new ContactMessageResponse(
                id, "Bob", "bob@example.com", null, "Refund inquiry", "When refund?",
                ContactMessageStatus.REPLIED, "We are resolving your issue.", Instant.now(), Instant.now()
        );

        when(contactMessageService.updateMessageStatusAndReply(eq(id), any(AdminReplyRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/notifications/contact-messages/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.status").value("REPLIED"))
                .andExpect(jsonPath("$.data.adminReplyNotes").value("We are resolving your issue."));
    }

    @Test
    @DisplayName("GET /api/v1/admin/notifications/store-info should return store info")
    void getStoreInfo_ShouldReturn200() throws Exception {
        StoreInfoSettingsResponse response = new StoreInfoSettingsResponse(
                UUID.randomUUID(), "Sale", "/sale", true, "[]", "7-day return", "3-5 days delivery",
                "Store Addr", "9-6", "info@fashionecommerce.com", "+919876543210", Instant.now()
        );
        when(storeInfoService.getStoreInfo()).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/notifications/store-info")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.supportEmail").value("info@fashionecommerce.com"));
    }
}

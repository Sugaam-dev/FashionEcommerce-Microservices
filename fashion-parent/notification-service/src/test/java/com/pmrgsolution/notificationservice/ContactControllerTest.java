package com.pmrgsolution.notificationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrgsolution.notificationservice.controller.ContactController;
import com.pmrgsolution.notificationservice.dto.request.ContactMessageRequest;
import com.pmrgsolution.notificationservice.dto.response.ContactMessageResponse;
import com.pmrgsolution.notificationservice.enums.ContactMessageStatus;
import com.pmrgsolution.notificationservice.service.ContactMessageService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ContactController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContactMessageService contactMessageService;

    @Test
    @DisplayName("POST /api/v1/contact should create contact message and return 201")
    void submitContactMessage_ShouldReturn201() throws Exception {
        ContactMessageRequest request = new ContactMessageRequest(
                "Alice",
                "alice@example.com",
                "+919876543210",
                "Product enquiry",
                "Is this dress available in M size?"
        );

        UUID generatedId = UUID.randomUUID();
        ContactMessageResponse response = new ContactMessageResponse(
                generatedId,
                "Alice",
                "alice@example.com",
                "+919876543210",
                "Product enquiry",
                "Is this dress available in M size?",
                ContactMessageStatus.NEW,
                null,
                null,
                Instant.now()
        );

        when(contactMessageService.submitMessage(any(ContactMessageRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").value(generatedId.toString()))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"));
    }
}

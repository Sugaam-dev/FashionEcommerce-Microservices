package com.pmrgsolution.notificationservice;

import com.pmrgsolution.notificationservice.controller.EmailStatusController;
import com.pmrgsolution.notificationservice.entity.EmailUsageQuota;
import com.pmrgsolution.notificationservice.repository.EmailUsageQuotaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EmailStatusController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmailStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailUsageQuotaRepository emailUsageQuotaRepository;

    @Test
    @DisplayName("GET /api/v1/email/status should return quota statistics")
    void getEmailStatus_ShouldReturn200() throws Exception {
        String todayKey = LocalDate.now(ZoneOffset.UTC).toString();
        EmailUsageQuota quota = EmailUsageQuota.builder()
                .dateKey(todayKey)
                .count(150)
                .dailyLimit(3000)
                .lastSentAt(Instant.now())
                .build();

        when(emailUsageQuotaRepository.findByDateKey(eq(todayKey))).thenReturn(Optional.of(quota));

        mockMvc.perform(get("/api/v1/email/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.count").value(150))
                .andExpect(jsonPath("$.data.dailyLimit").value(3000))
                .andExpect(jsonPath("$.data.remaining").value(2850));
    }
}

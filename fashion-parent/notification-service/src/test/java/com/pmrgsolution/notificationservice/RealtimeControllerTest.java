package com.pmrgsolution.notificationservice;

import com.pmrgsolution.notificationservice.controller.RealtimeController;
import com.pmrgsolution.notificationservice.service.SseHubService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RealtimeController.class)
@AutoConfigureMockMvc(addFilters = false)
class RealtimeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SseHubService sseHubService;

    @Test
    @DisplayName("GET /api/v1/realtime/admin/stream should establish SSE connection")
    void subscribeAdminStream_ShouldReturnOk() throws Exception {
        SseEmitter emitter = new SseEmitter(1000L);
        when(sseHubService.createEmitter()).thenReturn(emitter);

        mockMvc.perform(get("/api/v1/realtime/admin/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk());
    }
}

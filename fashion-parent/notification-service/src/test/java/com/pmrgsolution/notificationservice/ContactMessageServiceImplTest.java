package com.pmrgsolution.notificationservice;

import com.pmrgsolution.notificationservice.dto.request.AdminReplyRequest;
import com.pmrgsolution.notificationservice.dto.request.ContactMessageRequest;
import com.pmrgsolution.notificationservice.dto.response.ContactMessageResponse;
import com.pmrgsolution.notificationservice.entity.ContactMessage;
import com.pmrgsolution.notificationservice.enums.ContactMessageStatus;
import com.pmrgsolution.notificationservice.exception.ContactMessageNotFoundException;
import com.pmrgsolution.notificationservice.repository.ContactMessageRepository;
import com.pmrgsolution.notificationservice.service.ContactMessageServiceImpl;
import com.pmrgsolution.notificationservice.service.EmailSenderService;
import com.pmrgsolution.notificationservice.service.SseHubService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactMessageServiceImplTest {

    @Mock
    private ContactMessageRepository contactMessageRepository;

    @Mock
    private EmailSenderService emailSenderService;

    @Mock
    private SseHubService sseHubService;

    @InjectMocks
    private ContactMessageServiceImpl contactMessageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contactMessageService, "adminNotificationEmail", "admin@fashionecommerce.com");
    }

    @Test
    @DisplayName("Should submit contact message, notify admin via email, and broadcast via SSE")
    void submitMessage_ShouldSaveAndBroadcast() {
        ContactMessageRequest request = new ContactMessageRequest(
                "John Doe",
                "john@example.com",
                "+919876543210",
                "Order query",
                "Where is my order?"
        );

        UUID generatedId = UUID.randomUUID();
        ContactMessage saved = ContactMessage.builder()
                .id(generatedId)
                .name("John Doe")
                .email("john@example.com")
                .phone("+919876543210")
                .subject("Order query")
                .message("Where is my order?")
                .status(ContactMessageStatus.NEW)
                .createdAt(Instant.now())
                .build();

        when(contactMessageRepository.save(any(ContactMessage.class))).thenReturn(saved);

        ContactMessageResponse response = contactMessageService.submitMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(generatedId);
        assertThat(response.status()).isEqualTo(ContactMessageStatus.NEW);

        verify(emailSenderService).sendEmail(
                eq("admin@fashionecommerce.com"),
                eq("New Customer Inquiry: Order query"),
                eq("admin/new-inquiry-alert"),
                any()
        );

        verify(sseHubService).broadcast(eq("NEW_INQUIRY"), any(ContactMessageResponse.class));
    }

    @Test
    @DisplayName("Should reply to contact message and update status")
    void updateMessageStatusAndReply_ShouldUpdateEntity() {
        UUID id = UUID.randomUUID();
        ContactMessage message = ContactMessage.builder()
                .id(id)
                .name("John Doe")
                .email("john@example.com")
                .subject("Order query")
                .message("Where is my order?")
                .status(ContactMessageStatus.NEW)
                .createdAt(Instant.now())
                .build();

        AdminReplyRequest replyRequest = new AdminReplyRequest(
                ContactMessageStatus.REPLIED,
                "Your order has been shipped."
        );

        when(contactMessageRepository.findById(id)).thenReturn(Optional.of(message));
        when(contactMessageRepository.save(any(ContactMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ContactMessageResponse response = contactMessageService.updateMessageStatusAndReply(id, replyRequest);

        assertThat(response.status()).isEqualTo(ContactMessageStatus.REPLIED);
        assertThat(response.adminReplyNotes()).isEqualTo("Your order has been shipped.");
        assertThat(response.repliedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw ContactMessageNotFoundException when ID does not exist")
    void updateMessageStatusAndReply_WhenNotFound_ShouldThrowException() {
        UUID id = UUID.randomUUID();
        AdminReplyRequest replyRequest = new AdminReplyRequest(ContactMessageStatus.REPLIED, "Notes");

        when(contactMessageRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactMessageService.updateMessageStatusAndReply(id, replyRequest))
                .isInstanceOf(ContactMessageNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}

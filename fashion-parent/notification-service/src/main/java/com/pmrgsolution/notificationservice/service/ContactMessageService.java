package com.pmrgsolution.notificationservice.service;

import com.pmrgsolution.notificationservice.dto.request.AdminReplyRequest;
import com.pmrgsolution.notificationservice.dto.request.ContactMessageRequest;
import com.pmrgsolution.notificationservice.dto.response.ContactMessageResponse;
import com.pmrgsolution.notificationservice.enums.ContactMessageStatus;
import com.pmrgsolution.shared.core.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ContactMessageService {

    ContactMessageResponse submitMessage(ContactMessageRequest request);

    PageResponse<ContactMessageResponse> getMessages(ContactMessageStatus status, Pageable pageable);

    ContactMessageResponse updateMessageStatusAndReply(UUID id, AdminReplyRequest request);
}

package com.pmrgsolution.notificationservice.controller;

import com.pmrgsolution.notificationservice.dto.request.ContactMessageRequest;
import com.pmrgsolution.notificationservice.dto.response.ContactMessageResponse;
import com.pmrgsolution.notificationservice.service.ContactMessageService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contact")
@Tag(name = "Contact", description = "Public customer inquiry and contact endpoints")
public class ContactController {

    private final ContactMessageService contactMessageService;

    public ContactController(ContactMessageService contactMessageService) {
        this.contactMessageService = contactMessageService;
    }

    @PostMapping
    @Operation(summary = "Submit a customer contact message / inquiry")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> submitContactMessage(
            @Valid @RequestBody ContactMessageRequest request,
            HttpServletRequest httpServletRequest
    ) {
        ContactMessageResponse response = contactMessageService.submitMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        "Your inquiry has been received. Our team will get back to you soon.",
                        response,
                        httpServletRequest.getRequestURI()
                ));
    }
}

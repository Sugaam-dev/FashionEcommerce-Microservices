package com.pmrgsolution.notificationservice.exception;

import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;

public class TemplateNotFoundException extends ResourceNotFoundException {

    public TemplateNotFoundException(String message) {
        super(message);
    }
}

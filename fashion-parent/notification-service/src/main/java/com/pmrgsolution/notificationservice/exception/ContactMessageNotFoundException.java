package com.pmrgsolution.notificationservice.exception;

import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;

public class ContactMessageNotFoundException extends ResourceNotFoundException {

    public ContactMessageNotFoundException(String message) {
        super(message);
    }
}

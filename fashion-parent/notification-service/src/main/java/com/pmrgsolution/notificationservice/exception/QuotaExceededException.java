package com.pmrgsolution.notificationservice.exception;

import com.pmrgsolution.shared.core.exception.BadRequestException;

public class QuotaExceededException extends BadRequestException {

    public QuotaExceededException(String message) {
        super(message);
    }
}

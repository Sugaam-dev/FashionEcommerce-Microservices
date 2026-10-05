package com.pmrgsolution.authservice.exception;

import com.pmrgsolution.shared.core.exception.BadRequestException;

public class InvalidOtpException extends BadRequestException {

    public InvalidOtpException() {
        super("Invalid or incorrect OTP.");
    }

    public InvalidOtpException(String message) {
        super(message);
    }
}

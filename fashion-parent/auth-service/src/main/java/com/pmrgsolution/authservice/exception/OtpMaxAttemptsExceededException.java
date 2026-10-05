package com.pmrgsolution.authservice.exception;

import com.pmrgsolution.shared.core.exception.BadRequestException;

public class OtpMaxAttemptsExceededException extends BadRequestException {

    public OtpMaxAttemptsExceededException() {
        super("Maximum OTP attempts exceeded. Request a new one.");
    }

    public OtpMaxAttemptsExceededException(String message) {
        super(message);
    }
}

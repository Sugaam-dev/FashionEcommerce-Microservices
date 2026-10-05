package com.pmrgsolution.authservice.exception;

import com.pmrgsolution.shared.core.exception.BadRequestException;

public class OtpExpiredException extends BadRequestException {

    public OtpExpiredException() {
        super("OTP has expired. Please request a new one.");
    }

    public OtpExpiredException(String message) {
        super(message);
    }
}

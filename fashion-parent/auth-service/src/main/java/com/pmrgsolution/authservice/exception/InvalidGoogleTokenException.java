package com.pmrgsolution.authservice.exception;

import com.pmrgsolution.shared.core.exception.UnauthorizedException;

public class InvalidGoogleTokenException extends UnauthorizedException {

    public InvalidGoogleTokenException() {
        super("Google ID token validation failed.");
    }

    public InvalidGoogleTokenException(String message) {
        super(message);
    }
}

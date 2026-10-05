package com.pmrgsolution.authservice.exception;

import com.pmrgsolution.shared.core.exception.UnauthorizedException;

public class TokenReuseDetectedException extends UnauthorizedException {

    public TokenReuseDetectedException() {
        super("Security alert: session reuse detected. All sessions revoked.");
    }

    public TokenReuseDetectedException(String message) {
        super(message);
    }
}

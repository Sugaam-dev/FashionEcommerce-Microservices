package com.pmrgsolution.authservice.exception;

import com.pmrgsolution.shared.core.exception.UnauthorizedException;

public class AccountNotVerifiedException extends UnauthorizedException {

    public AccountNotVerifiedException() {
        super("Please verify your email before logging in.");
    }

    public AccountNotVerifiedException(String message) {
        super(message);
    }
}

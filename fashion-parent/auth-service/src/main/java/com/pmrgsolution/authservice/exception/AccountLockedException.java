package com.pmrgsolution.authservice.exception;

import com.pmrgsolution.shared.core.exception.UnauthorizedException;

import java.time.Instant;

public class AccountLockedException extends UnauthorizedException {

    public AccountLockedException() {
        super("Account is temporarily locked due to multiple failed login attempts.");
    }

    public AccountLockedException(String message) {
        super(message);
    }

    public AccountLockedException(Instant unlockTime) {
        super("Account is temporarily locked. Try again after " + unlockTime + ".");
    }
}

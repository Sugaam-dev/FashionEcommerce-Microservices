package com.pmrgsolution.shared.core.exception;

public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(403, "FORBIDDEN", message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(403, "FORBIDDEN", message, cause);
    }
}

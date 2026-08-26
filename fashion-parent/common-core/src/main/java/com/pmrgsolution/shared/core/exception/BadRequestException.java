package com.pmrgsolution.shared.core.exception;

public class BadRequestException extends BaseException {

    public BadRequestException(String message) {
        super(400, "BAD_REQUEST", message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(400, "BAD_REQUEST", message, cause);
    }
}

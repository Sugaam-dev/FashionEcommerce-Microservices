package com.pmrgsolution.shared.core.exception;
public class ConflictException extends BaseException {

    public ConflictException(String message) {
        super(409, "CONFLICT", message);
    }

    public ConflictException(String message, Throwable cause) {
        super(409, "CONFLICT", message, cause);
    }
}

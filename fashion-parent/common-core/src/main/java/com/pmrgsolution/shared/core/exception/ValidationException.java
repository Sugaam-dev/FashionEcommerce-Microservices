package com.pmrgsolution.shared.core.exception;

public class ValidationException extends BaseException {

    public ValidationException(String message) {
        super(422, "VALIDATION_ERROR", message);
    }

    public ValidationException(String message, Throwable cause) {
        super(422, "VALIDATION_ERROR", message, cause);
    }
}

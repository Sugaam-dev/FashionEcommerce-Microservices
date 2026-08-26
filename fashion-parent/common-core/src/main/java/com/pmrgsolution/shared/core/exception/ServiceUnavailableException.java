package com.pmrgsolution.shared.core.exception;

public class ServiceUnavailableException extends BaseException {

    public ServiceUnavailableException(String message) {
        super(503, "SERVICE_UNAVAILABLE", message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(503, "SERVICE_UNAVAILABLE", message, cause);
    }
}

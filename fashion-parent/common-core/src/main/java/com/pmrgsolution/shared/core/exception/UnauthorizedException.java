package com.pmrgsolution.shared.core.exception;
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(401, "UNAUTHORIZED", message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(401, "UNAUTHORIZED", message, cause);
    }
}

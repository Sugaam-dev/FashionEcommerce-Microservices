package com.pmrgsolution.shared.core.exception;

/**
 * Root of the platform exception hierarchy.
 * <p>
 * Every domain exception carries an {@link ErrorCode} that already knows
 * its HTTP status — subclasses never need to pass a raw integer again.
 */
public abstract class BaseException extends RuntimeException {

    private final ErrorCode errorCode;

    protected BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected BaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /** Convenience — delegates to {@link ErrorCode#httpStatus()}. */
    public int getStatusCode() {
        return errorCode.httpStatus();
    }
}

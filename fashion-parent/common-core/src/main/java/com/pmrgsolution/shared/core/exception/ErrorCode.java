package com.pmrgsolution.shared.core.exception;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Canonical error code registry for the entire platform.
 * <p>
 * Each constant carries its own HTTP status, so {@link BaseException} subclasses
 * only need to declare <em>which</em> error occurred — never the raw integer status.
 *
 * <p>Serialises to JSON as its {@link #name()} string, e.g. {@code "NOT_FOUND"}.
 */
public enum ErrorCode {

    // ─── 4xx Client Errors ────────────────────────────────────────────────────
    BAD_REQUEST(400),
    VALIDATION_FAILED(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    CONFLICT(409),
    UNPROCESSABLE_ENTITY(422),

    // ─── 5xx Server Errors ────────────────────────────────────────────────────
    INTERNAL_SERVER_ERROR(500),
    SERVICE_UNAVAILABLE(503);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    /** Returns the HTTP status code associated with this error. */
    public int httpStatus() {
        return httpStatus;
    }

    /** Serialises as the enum name (e.g. {@code "NOT_FOUND"}). */
    @JsonValue
    public String value() {
        return this.name();
    }
}

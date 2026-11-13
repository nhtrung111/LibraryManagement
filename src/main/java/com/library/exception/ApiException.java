package com.library.exception;

import java.util.Map;

import lombok.Getter;

/** Thrown by services for any business error. GlobalExceptionHandler turns it into JSON. */
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, Object> details;

    public ApiException(ErrorCode code) {
        this(code, code.defaultMessage(), Map.of());
    }

    public ApiException(ErrorCode code, String message) {
        this(code, message, Map.of());
    }

    public ApiException(ErrorCode code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details;
    }
}

package com.library.exception;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.library.dto.ErrorResponse;

import lombok.RequiredArgsConstructor;

/** Builds ErrorResponse objects. Shared by the exception handler and the security handlers. */
@Component
@RequiredArgsConstructor
public class ErrorResponses {

    private final Clock clock;

    public ErrorResponse of(ErrorCode code, String message, Map<String, Object> details) {
        return new ErrorResponse(code.status(), code.name(), message,
                details == null ? Map.of() : details, OffsetDateTime.now(clock));
    }

    public ErrorResponse of(ErrorCode code) {
        return of(code, code.defaultMessage(), Map.of());
    }
}

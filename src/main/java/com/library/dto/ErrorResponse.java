package com.library.dto;

import java.time.OffsetDateTime;
import java.util.Map;

/** The single error shape for every failed request (constitution rule 7). */
public record ErrorResponse(
        int status,
        String code,
        String message,
        Map<String, Object> details,
        OffsetDateTime timestamp) {
}

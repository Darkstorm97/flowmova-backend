package com.flowmova.backend.shared.api;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<ApiFieldError> fieldErrors) {

    public ApiErrorResponse(
            Instant timestamp,
            int status,
            String error,
            String code,
            String message,
            String path) {
        this(timestamp, status, error, code, message, path, List.of());
    }
}

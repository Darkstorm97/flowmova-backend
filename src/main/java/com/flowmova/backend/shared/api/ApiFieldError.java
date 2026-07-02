package com.flowmova.backend.shared.api;

public record ApiFieldError(
        String field,
        String message) {
}

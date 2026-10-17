package com.cooperative.voting.dto;

import lombok.Getter;

import java.util.List;

/**
 * Consistent error response body: {"error": "message", "details": [...]}.
 */
@Getter
public class ErrorResponse {

    private final String error;
    private final List<String> details;

    public ErrorResponse(String error) {
        this(error, List.of());
    }

    public ErrorResponse(String error, List<String> details) {
        this.error = error;
        this.details = details;
    }
}

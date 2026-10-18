package com.example.pauta.dto;

import java.time.Instant;

public record ErrorResponse(String error, Instant timestamp, int status) {

    public static ErrorResponse of(String error, int status) {
        return new ErrorResponse(error, Instant.now(), status);
    }
}

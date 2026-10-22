package com.acme.votacao.pauta;

import java.time.Instant;

public record ErroResponse(Instant timestamp, int status, String error, String message, String path) {

    public static ErroResponse of(Instant timestamp, org.springframework.http.HttpStatus status, String message, String path) {
        return new ErroResponse(timestamp, status.value(), status.getReasonPhrase(), message, path);
    }
}

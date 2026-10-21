package com.votacao.infrastructure.error;

import java.time.LocalDateTime;

public record ErrorResponse(String error, LocalDateTime timestamp, String path) {
}

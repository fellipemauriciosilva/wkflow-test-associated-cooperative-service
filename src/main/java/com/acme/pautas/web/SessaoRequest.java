package com.acme.pautas.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SessaoRequest(
        @Min(value = 1, message = "deve ser maior que zero")
        @Max(value = 1440, message = "deve ser no máximo 1440")
        Integer duracaoMinutos) {
}

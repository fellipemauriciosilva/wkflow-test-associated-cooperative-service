package com.acme.pautas.web;

import jakarta.validation.constraints.Min;

public record SessaoRequest(
        @Min(value = 1, message = "deve ser maior que zero")
        Integer duracaoMinutos) {
}

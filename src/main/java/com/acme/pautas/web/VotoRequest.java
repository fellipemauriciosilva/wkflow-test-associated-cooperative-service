package com.acme.pautas.web;

import com.acme.pautas.domain.OpcaoVoto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VotoRequest(
        @NotBlank(message = "não pode ser vazio")
        String associadoId,

        @NotNull(message = "é obrigatório")
        OpcaoVoto voto) {
}

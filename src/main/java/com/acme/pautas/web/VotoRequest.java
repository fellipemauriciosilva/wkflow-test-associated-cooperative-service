package com.acme.pautas.web;

import com.acme.pautas.domain.OpcaoVoto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VotoRequest(
        @NotBlank(message = "não pode ser vazio")
        @Size(max = 255, message = "deve ter no máximo 255 caracteres")
        String associadoId,

        @NotNull(message = "é obrigatório")
        OpcaoVoto voto) {
}

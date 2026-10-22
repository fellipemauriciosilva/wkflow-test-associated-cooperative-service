package com.acme.votacao.pauta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VotoRequest(
        @NotBlank(message = "associadoId é obrigatório")
        String associadoId,
        @NotNull(message = "voto é obrigatório")
        VotoValor voto
) {
}

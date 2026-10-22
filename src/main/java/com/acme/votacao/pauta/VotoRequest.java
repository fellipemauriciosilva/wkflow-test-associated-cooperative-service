package com.acme.votacao.pauta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VotoRequest(
        @NotBlank(message = "associadoId é obrigatório")
        @Size(max = 255, message = "associadoId deve ter no máximo 255 caracteres")
        String associadoId,
        @NotNull(message = "voto é obrigatório")
        VotoValor voto
) {
}

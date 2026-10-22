package com.acme.votacao.pauta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PautaRequest(
        @NotBlank(message = "titulo é obrigatório")
        @Size(max = 255, message = "titulo deve ter no máximo 255 caracteres")
        String titulo,
        String descricao
) {
}

package com.votacao.api.pauta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PautaRequest(
        @NotBlank(message = "titulo é obrigatório")
        @Size(max = 255, message = "titulo deve ter no máximo 255 caracteres") String titulo,
        @Size(max = 255, message = "descricao deve ter no máximo 255 caracteres") String descricao) {
}

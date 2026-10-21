package com.votacao.api.pauta.dto;

import jakarta.validation.constraints.NotBlank;

public record PautaRequest(
        @NotBlank(message = "titulo é obrigatório") String titulo,
        String descricao) {
}

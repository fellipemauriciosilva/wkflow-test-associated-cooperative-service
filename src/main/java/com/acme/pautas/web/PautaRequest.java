package com.acme.pautas.web;

import jakarta.validation.constraints.NotBlank;

public record PautaRequest(
        @NotBlank(message = "não pode ser vazio") String titulo,
        String descricao) {
}

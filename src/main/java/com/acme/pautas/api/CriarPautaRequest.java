package com.acme.pautas.api;

import jakarta.validation.constraints.NotBlank;

public record CriarPautaRequest(@NotBlank String titulo, String descricao) {
}

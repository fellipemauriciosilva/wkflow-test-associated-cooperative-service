package com.acme.pautas.pauta;

import jakarta.validation.constraints.NotBlank;

public record CriarPautaRequest(@NotBlank String titulo, String descricao) {
}

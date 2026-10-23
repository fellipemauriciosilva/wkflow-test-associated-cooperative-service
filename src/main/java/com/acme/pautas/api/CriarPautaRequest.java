package com.acme.pautas.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarPautaRequest(
        @NotBlank @Size(max = 255) String titulo,
        @Size(max = 2000) String descricao) {
}

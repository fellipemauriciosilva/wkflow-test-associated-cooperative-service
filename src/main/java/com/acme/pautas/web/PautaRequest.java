package com.acme.pautas.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PautaRequest(
        @NotBlank(message = "não pode ser vazio")
        @Size(max = 255, message = "tamanho máximo de 255 caracteres")
        String titulo,
        @Size(max = 2000, message = "tamanho máximo de 2000 caracteres")
        String descricao) {
}

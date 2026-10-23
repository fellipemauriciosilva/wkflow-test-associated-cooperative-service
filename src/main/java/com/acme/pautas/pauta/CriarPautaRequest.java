package com.acme.pautas.pauta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarPautaRequest(
        @NotBlank(message = "obrigatório e não pode ser vazio")
        @Size(max = 255, message = "máximo de 255 caracteres")
        String titulo,

        @Size(max = 2000, message = "máximo de 2000 caracteres")
        String descricao) {
}

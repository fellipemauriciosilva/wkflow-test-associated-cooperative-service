package br.acme.pauta;

import jakarta.validation.constraints.NotBlank;

public record PautaRequest(
        @NotBlank(message = "obrigatorio")
        String titulo,
        String descricao) {
}

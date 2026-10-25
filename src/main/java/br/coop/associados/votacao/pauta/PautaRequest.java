package br.coop.associados.votacao.pauta;

import jakarta.validation.constraints.NotBlank;

public record PautaRequest(
        @NotBlank String titulo,
        String descricao) {
}

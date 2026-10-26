package br.acme.wkflow.pauta;

import jakarta.validation.constraints.NotBlank;

public record PautaRequest(@NotBlank String descricao) {
}

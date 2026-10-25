package br.coop.pautas.pauta;

import jakarta.validation.constraints.NotBlank;

public record PautaRequest(@NotBlank String titulo, String descricao) {
}

package br.coop.pautas.pauta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PautaRequest(
        @NotBlank @Size(max = 255) String titulo,
        @Size(max = 255) String descricao) {
}

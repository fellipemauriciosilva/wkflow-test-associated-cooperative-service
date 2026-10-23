package br.coop.assembleia.pauta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PautaRequest(
        @NotBlank
        @Size(max = Pauta.TITULO_MAX)
        String titulo,

        @Size(max = Pauta.DESCRICAO_MAX)
        String descricao) {
}

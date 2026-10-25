package br.coop.associados.votacao.pauta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PautaRequest(
        @NotBlank @Size(max = PautaRequest.TITULO_MAX) String titulo,
        @Size(max = PautaRequest.DESCRICAO_MAX) String descricao) {

    public static final int TITULO_MAX = 255;
    public static final int DESCRICAO_MAX = 1000;
}

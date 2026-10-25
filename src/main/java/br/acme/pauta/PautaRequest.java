package br.acme.pauta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PautaRequest(
        @NotBlank(message = "obrigatorio")
        @Size(max = Pauta.TAMANHO_MAXIMO, message = "tamanho maximo " + Pauta.TAMANHO_MAXIMO)
        String titulo,
        @Size(max = Pauta.TAMANHO_MAXIMO, message = "tamanho maximo " + Pauta.TAMANHO_MAXIMO)
        String descricao) {

    public PautaRequest {
        titulo = titulo == null ? null : titulo.strip();
        descricao = descricao == null || descricao.isBlank() ? null : descricao.strip();
    }
}

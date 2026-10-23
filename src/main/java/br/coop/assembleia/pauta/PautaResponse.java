package br.coop.assembleia.pauta;

import java.util.UUID;

public record PautaResponse(UUID id, String titulo, String descricao) {

    static PautaResponse from(Pauta pauta) {
        return new PautaResponse(pauta.getId(), pauta.getTitulo(), pauta.getDescricao());
    }
}

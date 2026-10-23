package com.acme.pautas.pauta;

public record PautaResponse(Long id, String titulo, String descricao) {

    static PautaResponse de(Pauta pauta) {
        return new PautaResponse(pauta.getId(), pauta.getTitulo(), pauta.getDescricao());
    }
}

package com.acme.votacao.pauta;

public record PautaResponse(Long id, String titulo, String descricao) {

    public static PautaResponse from(Pauta pauta) {
        return new PautaResponse(pauta.getId(), pauta.getTitulo(), pauta.getDescricao());
    }
}

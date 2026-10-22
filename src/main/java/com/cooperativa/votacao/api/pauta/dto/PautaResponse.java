package com.cooperativa.votacao.api.pauta.dto;

import com.cooperativa.votacao.domain.pauta.Pauta;

public record PautaResponse(Long id, String titulo, String descricao) {

    public static PautaResponse de(Pauta pauta) {
        return new PautaResponse(pauta.getId(), pauta.getTitulo(), pauta.getDescricao());
    }
}

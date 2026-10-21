package com.votacao.api.pauta.dto;

import com.votacao.domain.pauta.Pauta;
import java.time.LocalDateTime;

public record PautaResponse(
        Long id,
        String titulo,
        String descricao,
        LocalDateTime criacao) {

    public static PautaResponse from(Pauta pauta) {
        return new PautaResponse(pauta.getId(), pauta.getTitulo(), pauta.getDescricao(), pauta.getCriacao());
    }
}

package com.votacao.api.voto.dto;

import com.votacao.domain.voto.Voto;
import java.time.LocalDateTime;

public record VotoResponse(
        Long id,
        Long pautaId,
        Long idAssociado,
        String voto,
        LocalDateTime criacao) {

    public static VotoResponse from(Voto voto) {
        return new VotoResponse(
                voto.getId(),
                voto.getPauta().getId(),
                voto.getIdAssociado(),
                voto.getTipo().name(),
                voto.getCriacao());
    }
}

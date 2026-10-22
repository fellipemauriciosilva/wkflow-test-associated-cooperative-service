package com.cooperativa.votacao.api.voto.dto;

import com.cooperativa.votacao.domain.voto.Voto;

public record VotoResponse(Long id, Long pautaId, String associado, String voto) {

    public static VotoResponse de(Voto voto) {
        return new VotoResponse(
                voto.getId(), voto.getPauta().getId(), voto.getAssociado(), voto.getTipoVoto().name());
    }
}

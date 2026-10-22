package com.cooperativa.votacao.api.voto.dto;

import com.cooperativa.votacao.domain.voto.ResultadoVotacao;

public record ResultadoResponse(long sim, long nao, String situacao) {

    public static ResultadoResponse de(ResultadoVotacao resultado) {
        return new ResultadoResponse(resultado.sim(), resultado.nao(), resultado.situacao());
    }
}

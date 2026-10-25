package com.acme.pautas.web;

import com.acme.pautas.service.ResultadoVotacao;

public record ResultadoResponse(long sim, long nao) {

    static ResultadoResponse de(ResultadoVotacao resultado) {
        return new ResultadoResponse(resultado.sim(), resultado.nao());
    }
}

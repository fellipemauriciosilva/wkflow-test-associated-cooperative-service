package com.acme.pautas.web;

import com.acme.pautas.domain.OpcaoVoto;
import com.acme.pautas.domain.Voto;

public record VotoResponse(Long id, Long pautaId, String associadoId, OpcaoVoto voto) {

    static VotoResponse de(Voto voto) {
        return new VotoResponse(voto.getId(), voto.getPautaId(), voto.getAssociadoId(), voto.getOpcao());
    }
}

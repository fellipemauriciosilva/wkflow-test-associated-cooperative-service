package com.acme.votacao.pauta;

import java.time.Instant;

public record SessaoResponse(Long id, Long pautaId, Instant inicio, Instant fim, Integer duracaoMinutos) {

    public static SessaoResponse from(Sessao sessao) {
        return new SessaoResponse(sessao.getId(), sessao.getPautaId(), sessao.getInicio(), sessao.getFim(), sessao.getDuracaoMinutos());
    }
}

package com.cooperativa.votacao.api.sessao.dto;

import com.cooperativa.votacao.domain.sessao.Sessao;
import java.time.Instant;

public record SessaoResponse(Long id, Long pautaId, Integer duracaoMinutos, Instant abertura, Instant fimEm) {

    public static SessaoResponse de(Sessao sessao) {
        return new SessaoResponse(
                sessao.getId(),
                sessao.getPauta().getId(),
                sessao.getDuracaoMinutos(),
                sessao.getAbertura(),
                sessao.getFim());
    }
}

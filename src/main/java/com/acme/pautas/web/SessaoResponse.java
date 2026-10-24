package com.acme.pautas.web;

import com.acme.pautas.domain.Sessao;

import java.time.Instant;

public record SessaoResponse(Long id, Long pautaId, Instant abertura, Instant fim) {

    static SessaoResponse de(Sessao sessao) {
        return new SessaoResponse(sessao.getId(), sessao.getPautaId(), sessao.getAbertura(), sessao.getFim());
    }
}

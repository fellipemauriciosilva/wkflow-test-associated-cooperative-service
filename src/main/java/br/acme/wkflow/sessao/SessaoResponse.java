package br.acme.wkflow.sessao;

import java.time.Instant;

public record SessaoResponse(Long pautaId, Instant inicio, Instant fim) {

    static SessaoResponse de(Sessao sessao) {
        return new SessaoResponse(sessao.getPautaId(), sessao.getInicio(), sessao.getFim());
    }
}

package com.votacao.api.sessao.dto;

import com.votacao.domain.sessao.Sessao;
import java.time.LocalDateTime;

public record SessaoResponse(
        Long id,
        Long pautaId,
        LocalDateTime abertura,
        LocalDateTime fechamento,
        String status) {

    public static SessaoResponse from(Sessao sessao) {
        return new SessaoResponse(
                sessao.getId(),
                sessao.getPauta().getId(),
                sessao.getAbertura(),
                sessao.getFechamento(),
                "ABERTA");
    }
}

package br.acme.pauta;

import java.time.Duration;
import java.time.Instant;

public record SessaoResponse(Long id, Long pautaId, Instant inicio, Instant fim, long duracaoMinutos) {

    static SessaoResponse de(Sessao sessao) {
        long minutos = Duration.between(sessao.getInicio(), sessao.getFim()).toMinutes();
        return new SessaoResponse(sessao.getId(), sessao.getPauta().getId(), sessao.getInicio(), sessao.getFim(), minutos);
    }
}

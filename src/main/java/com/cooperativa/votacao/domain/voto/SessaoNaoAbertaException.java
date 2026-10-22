package com.cooperativa.votacao.domain.voto;

public class SessaoNaoAbertaException extends RuntimeException {

    public SessaoNaoAbertaException(Long pautaId) {
        super("pauta " + pautaId + " nao possui sessao aberta");
    }
}

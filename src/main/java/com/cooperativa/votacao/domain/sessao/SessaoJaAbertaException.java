package com.cooperativa.votacao.domain.sessao;

public class SessaoJaAbertaException extends RuntimeException {

    public SessaoJaAbertaException(Long pautaId) {
        super("pauta " + pautaId + " ja possui sessao aberta");
    }
}

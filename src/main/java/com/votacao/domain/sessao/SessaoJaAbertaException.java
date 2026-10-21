package com.votacao.domain.sessao;

public class SessaoJaAbertaException extends RuntimeException {

    public SessaoJaAbertaException(Long pautaId) {
        super("Sessão já existente para a pauta: " + pautaId);
    }
}

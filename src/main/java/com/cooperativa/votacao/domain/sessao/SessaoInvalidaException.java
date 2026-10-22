package com.cooperativa.votacao.domain.sessao;

public class SessaoInvalidaException extends RuntimeException {

    public SessaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}

package com.cooperativa.votacao.domain.voto;

public class VotoInvalidoException extends RuntimeException {

    public VotoInvalidoException(String mensagem) {
        super(mensagem);
    }
}

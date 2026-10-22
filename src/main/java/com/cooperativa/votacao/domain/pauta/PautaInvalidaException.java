package com.cooperativa.votacao.domain.pauta;

public class PautaInvalidaException extends RuntimeException {

    public PautaInvalidaException(String mensagem) {
        super(mensagem);
    }
}

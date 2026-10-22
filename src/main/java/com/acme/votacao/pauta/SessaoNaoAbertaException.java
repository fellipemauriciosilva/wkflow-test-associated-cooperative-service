package com.acme.votacao.pauta;

public class SessaoNaoAbertaException extends RuntimeException {

    public SessaoNaoAbertaException(Long pautaId) {
        super("Sessão não está aberta para votação na pauta: " + pautaId);
    }
}

package com.acme.votacao.pauta;

public class SessaoJaAbertaException extends RuntimeException {

    public SessaoJaAbertaException(Long pautaId) {
        super("Já existe sessão aberta para a pauta: " + pautaId);
    }
}

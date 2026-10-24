package com.acme.pautas.service;

public class SessaoJaAbertaException extends RuntimeException {

    public SessaoJaAbertaException(Long pautaId) {
        super("Pauta " + pautaId + " já possui sessão aberta.");
    }
}

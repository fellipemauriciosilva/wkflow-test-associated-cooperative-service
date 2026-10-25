package com.acme.pautas.service;

public class SemSessaoAbertaException extends RuntimeException {

    public SemSessaoAbertaException(Long pautaId) {
        super("Pauta " + pautaId + " não possui sessão aberta.");
    }
}

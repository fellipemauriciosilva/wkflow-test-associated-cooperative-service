package com.acme.pautas.service;

public class VotacaoIndisponivelException extends RuntimeException {

    public VotacaoIndisponivelException() {
        super("votação indisponível neste ambiente");
    }
}

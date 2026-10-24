package com.acme.pautas.service;

public class PautaNaoEncontradaException extends RuntimeException {

    public PautaNaoEncontradaException(Long id) {
        super("Pauta " + id + " não encontrada.");
    }
}

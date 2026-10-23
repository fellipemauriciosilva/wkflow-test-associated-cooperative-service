package com.acme.pautas.domain;

public class PautaNaoEncontradaException extends RuntimeException {

    public PautaNaoEncontradaException(Long id) {
        super("pauta nao encontrada: " + id);
    }
}

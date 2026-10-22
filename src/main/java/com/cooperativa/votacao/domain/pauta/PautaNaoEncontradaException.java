package com.cooperativa.votacao.domain.pauta;

public class PautaNaoEncontradaException extends RuntimeException {

    public PautaNaoEncontradaException(Long id) {
        super("Pauta nao encontrada");
    }
}

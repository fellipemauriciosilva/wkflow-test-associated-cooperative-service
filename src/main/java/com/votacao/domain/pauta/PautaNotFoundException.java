package com.votacao.domain.pauta;

public class PautaNotFoundException extends RuntimeException {

    public PautaNotFoundException(Long id) {
        super("Pauta não encontrada: " + id);
    }
}

package com.acme.pautas.service;

public class VotoDuplicadoException extends RuntimeException {

    public VotoDuplicadoException(Long pautaId, String associadoId) {
        super("Associado " + associadoId + " já votou na pauta " + pautaId);
    }

    public VotoDuplicadoException(Long pautaId, String associadoId, Throwable causa) {
        super("Associado " + associadoId + " já votou na pauta " + pautaId, causa);
    }
}

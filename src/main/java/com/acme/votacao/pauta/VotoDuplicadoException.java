package com.acme.votacao.pauta;

public class VotoDuplicadoException extends RuntimeException {

    public VotoDuplicadoException(Long pautaId, String associadoId) {
        super("Associado " + associadoId + " já votou na pauta: " + pautaId);
    }
}

package com.cooperativa.votacao.domain.voto;

public class VotoDuplicadoException extends RuntimeException {

    public VotoDuplicadoException(Long pautaId, String associado) {
        super("associado " + associado + " ja votou na pauta " + pautaId);
    }
}

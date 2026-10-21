package com.votacao.domain.voto;

public class VotoDuplicadoException extends RuntimeException {

    public VotoDuplicadoException(Long idAssociado, Long pautaId) {
        super("Associado já votou nesta pauta (duplicata): associado=" + idAssociado + ", pauta=" + pautaId);
    }
}

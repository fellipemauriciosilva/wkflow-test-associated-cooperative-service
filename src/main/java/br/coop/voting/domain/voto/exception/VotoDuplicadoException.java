package br.coop.voting.domain.voto.exception;

public class VotoDuplicadoException extends RuntimeException {
    public VotoDuplicadoException(Long pautaId, String idAssociado) {
        super("Associado " + idAssociado + " já votou na pauta " + pautaId);
    }
}

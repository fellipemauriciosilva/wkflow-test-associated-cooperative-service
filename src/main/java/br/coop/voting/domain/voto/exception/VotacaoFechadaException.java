package br.coop.voting.domain.voto.exception;

public class VotacaoFechadaException extends RuntimeException {
    public VotacaoFechadaException(Long pautaId) {
        super("Votação não está aberta para a pauta " + pautaId);
    }
}

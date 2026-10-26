package br.acme.wkflow.voto;

public class VotoDuplicadoException extends RuntimeException {

    public VotoDuplicadoException(Long pautaId, String associadoId) {
        super("Associado " + associadoId + " ja votou na pauta " + pautaId);
    }
}

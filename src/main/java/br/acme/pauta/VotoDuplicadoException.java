package br.acme.pauta;

public class VotoDuplicadoException extends RuntimeException {

    public VotoDuplicadoException(Long pautaId) {
        super("Associado ja votou na pauta: " + pautaId);
    }
}

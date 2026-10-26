package br.acme.wkflow.voto;

public class VotoDuplicadoException extends RuntimeException {

    public VotoDuplicadoException() {
        super("Associado ja votou nesta pauta");
    }
}

package br.coop.voting.shared.exception;

public class PautaNaoEncontradaException extends RuntimeException {

    public PautaNaoEncontradaException(String message) {
        super(message);
    }

    public PautaNaoEncontradaException(String message, Throwable cause) {
        super(message, cause);
    }
}

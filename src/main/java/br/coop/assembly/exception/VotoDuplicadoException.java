package br.coop.assembly.exception;

public class VotoDuplicadoException extends RuntimeException {
    private final String code = "VOTO_DUPLICADO";

    public VotoDuplicadoException(String message) {
        super(message);
    }

    public VotoDuplicadoException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getCode() {
        return code;
    }
}

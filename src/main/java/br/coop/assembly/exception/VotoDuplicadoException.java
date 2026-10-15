package br.coop.assembly.exception;

public class VotoDuplicadoException extends RuntimeException {
    private final String code;

    public VotoDuplicadoException(String message) {
        super(message);
        this.code = "VOTO_DUPLICADO";
    }

    public String getCode() {
        return code;
    }
}

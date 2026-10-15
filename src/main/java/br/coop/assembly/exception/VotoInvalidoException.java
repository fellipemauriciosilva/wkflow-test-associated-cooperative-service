package br.coop.assembly.exception;

public class VotoInvalidoException extends RuntimeException {
    private final String code;

    public VotoInvalidoException(String message) {
        super(message);
        this.code = "VOTO_INVALIDO";
    }

    public String getCode() {
        return code;
    }
}

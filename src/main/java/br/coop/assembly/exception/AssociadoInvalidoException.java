package br.coop.assembly.exception;

public class AssociadoInvalidoException extends RuntimeException {
    private final String code;

    public AssociadoInvalidoException(String message) {
        super(message);
        this.code = "ASSOCIADO_INVALIDO";
    }

    public String getCode() {
        return code;
    }
}

package br.coop.assembly.exception;

public class DuracaoInvalidaException extends RuntimeException {
    private final String code = "DURACAO_INVALIDA";

    public DuracaoInvalidaException(String message) {
        super(message);
    }

    public String getCode() {
        return code;
    }
}

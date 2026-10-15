package br.coop.assembly.exception;

public class PautaValidationException extends RuntimeException {
    private final String code = "PAUTA_VALIDATION_ERROR";

    public PautaValidationException(String message) {
        super(message);
    }

    public String getCode() {
        return code;
    }
}

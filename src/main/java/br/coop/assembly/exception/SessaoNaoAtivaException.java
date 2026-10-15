package br.coop.assembly.exception;

public class SessaoNaoAtivaException extends RuntimeException {
    private final String code = "SESSION_NOT_ACTIVE";

    public SessaoNaoAtivaException(String message) {
        super(message);
    }

    public SessaoNaoAtivaException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getCode() {
        return code;
    }
}

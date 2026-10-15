package br.coop.assembly.exception;

public class SessaoNaoAtivaException extends RuntimeException {
    private final String code;

    public SessaoNaoAtivaException(String message) {
        super(message);
        this.code = "SESSAO_NAO_ATIVA";
    }

    public String getCode() {
        return code;
    }
}

package br.coop.assembly.exception;

public class SessaoNaoEncontradaException extends RuntimeException {
    private final String code = "SESSAO_NAO_ENCONTRADA";
    private final Long id;

    public SessaoNaoEncontradaException(Long id) {
        super("Sessao com id " + id + " nao encontrada");
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public Long getId() {
        return id;
    }
}

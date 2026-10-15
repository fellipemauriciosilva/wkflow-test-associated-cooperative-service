package br.coop.assembly.exception;

public class PautaNotFoundException extends RuntimeException {
    private final String code = "PAUTA_NOT_FOUND";
    private final Long id;

    public PautaNotFoundException(Long id) {
        super("Pauta com id " + id + " não encontrada");
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public Long getId() {
        return id;
    }
}

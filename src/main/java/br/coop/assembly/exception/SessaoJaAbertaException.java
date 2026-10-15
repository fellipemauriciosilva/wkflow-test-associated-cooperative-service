package br.coop.assembly.exception;

public class SessaoJaAbertaException extends RuntimeException {
    private final String code = "SESSAO_JA_ABERTA";
    private final Long pautaId;

    public SessaoJaAbertaException(Long pautaId) {
        super("Sessao ja aberta para esta pauta");
        this.pautaId = pautaId;
    }

    public String getCode() {
        return code;
    }

    public Long getPautaId() {
        return pautaId;
    }
}

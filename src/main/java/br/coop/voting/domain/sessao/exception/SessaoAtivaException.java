package br.coop.voting.domain.sessao.exception;

public class SessaoAtivaException extends RuntimeException {

    public SessaoAtivaException(Long pautaId) {
        super("Pauta " + pautaId + " já possui sessão ativa");
    }
}

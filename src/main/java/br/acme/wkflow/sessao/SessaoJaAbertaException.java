package br.acme.wkflow.sessao;

public class SessaoJaAbertaException extends RuntimeException {

    public SessaoJaAbertaException(Long pautaId) {
        super("Pauta " + pautaId + " ja possui sessao aberta");
    }
}

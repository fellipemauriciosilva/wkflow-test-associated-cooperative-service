package br.acme.wkflow.voto;

public class SessaoNaoAbertaException extends RuntimeException {

    public SessaoNaoAbertaException(Long pautaId) {
        super("Pauta " + pautaId + " nao possui sessao aberta");
    }
}

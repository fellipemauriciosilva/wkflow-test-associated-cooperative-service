package br.acme.pauta;

public class SessaoAbertaException extends RuntimeException {

    public SessaoAbertaException(Long pautaId) {
        super("Pauta ja possui sessao aberta: " + pautaId);
    }
}

package br.acme.pauta;

public class SemSessaoAbertaException extends RuntimeException {

    public SemSessaoAbertaException(Long pautaId) {
        super("Pauta sem sessao aberta: " + pautaId);
    }
}

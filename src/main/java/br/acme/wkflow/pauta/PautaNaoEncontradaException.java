package br.acme.wkflow.pauta;

public class PautaNaoEncontradaException extends RuntimeException {

    public PautaNaoEncontradaException(Long id) {
        super("Pauta " + id + " nao encontrada");
    }
}

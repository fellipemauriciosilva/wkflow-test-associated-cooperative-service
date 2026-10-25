package br.acme.pauta;

public class PautaNaoEncontradaException extends RuntimeException {

    public PautaNaoEncontradaException(Long id) {
        super("Pauta nao encontrada: " + id);
    }
}

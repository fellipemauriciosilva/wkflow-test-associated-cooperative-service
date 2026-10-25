package br.coop.associados.votacao.pauta;

public class PautaNaoEncontradaException extends RuntimeException {

    public PautaNaoEncontradaException(Long id) {
        super("Pauta não encontrada: " + id);
    }
}

package pautas;

public class PautaNaoEncontradaException extends RuntimeException {

    public PautaNaoEncontradaException(Long id) {
        super("Pauta " + id + " não encontrada");
    }
}

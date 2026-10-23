package pautas;

public class SessaoJaExisteException extends RuntimeException {

    public SessaoJaExisteException(Long pautaId) {
        super("Pauta " + pautaId + " já possui sessão de votação");
    }
}

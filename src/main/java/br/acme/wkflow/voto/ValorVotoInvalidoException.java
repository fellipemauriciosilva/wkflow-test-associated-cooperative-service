package br.acme.wkflow.voto;

public class ValorVotoInvalidoException extends RuntimeException {

    public ValorVotoInvalidoException() {
        super("Valor de voto invalido");
    }
}

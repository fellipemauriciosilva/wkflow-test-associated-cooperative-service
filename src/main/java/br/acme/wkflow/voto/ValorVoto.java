package br.acme.wkflow.voto;

public enum ValorVoto {
    SIM,
    NAO;

    public static ValorVoto de(String texto) {
        for (ValorVoto valor : values()) {
            if (valor.name().equals(texto)) {
                return valor;
            }
        }
        throw new ValorVotoInvalidoException();
    }
}

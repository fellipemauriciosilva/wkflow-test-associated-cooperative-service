package br.acme.pauta;

public class ResultadoIndisponivelException extends RuntimeException {

    public ResultadoIndisponivelException(Long pautaId) {
        super("Resultado indisponivel: sessao da pauta ainda aberta: " + pautaId);
    }
}

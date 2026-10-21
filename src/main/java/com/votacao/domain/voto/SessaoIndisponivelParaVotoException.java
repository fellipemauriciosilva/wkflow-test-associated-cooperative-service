package com.votacao.domain.voto;

public class SessaoIndisponivelParaVotoException extends RuntimeException {

    public SessaoIndisponivelParaVotoException(Long pautaId) {
        super("Sessão de votação inválida (não iniciada, fechada ou expirada) para a pauta: " + pautaId);
    }
}

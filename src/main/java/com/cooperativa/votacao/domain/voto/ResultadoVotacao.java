package com.cooperativa.votacao.domain.voto;

/**
 * Agregado calculado (nao persistido). situacao: "aberta" | "encerrada" | "nao_iniciada" —
 * ultimo caso quando a pauta nunca teve sessao (dúvida 2 do task.md, sem resposta do PO;
 * ver docs/CONTEXT.md).
 */
public record ResultadoVotacao(long sim, long nao, String situacao) {

    public static final String SITUACAO_ABERTA = "aberta";
    public static final String SITUACAO_ENCERRADA = "encerrada";
    public static final String SITUACAO_NAO_INICIADA = "nao_iniciada";
}

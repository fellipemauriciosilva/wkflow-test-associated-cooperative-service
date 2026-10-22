package com.acme.votacao.pauta;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

public record SessaoRequest(
        @Positive(message = "duracaoMinutos deve ser maior que zero")
        @Max(value = SessaoRequest.DURACAO_MAXIMA_MINUTOS, message = "duracaoMinutos não pode exceder " + SessaoRequest.DURACAO_MAXIMA_MINUTOS)
        Integer duracaoMinutos
) {

    public static final int DURACAO_PADRAO_MINUTOS = 1;
    public static final int DURACAO_MAXIMA_MINUTOS = 1440;

    public int duracaoMinutosOuPadrao() {
        return duracaoMinutos != null ? duracaoMinutos : DURACAO_PADRAO_MINUTOS;
    }
}

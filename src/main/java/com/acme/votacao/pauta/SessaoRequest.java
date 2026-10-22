package com.acme.votacao.pauta;

import jakarta.validation.constraints.Positive;

public record SessaoRequest(
        @Positive(message = "duracaoMinutos deve ser maior que zero")
        Integer duracaoMinutos
) {

    public static final int DURACAO_PADRAO_MINUTOS = 1;

    public int duracaoMinutosOuPadrao() {
        return duracaoMinutos != null ? duracaoMinutos : DURACAO_PADRAO_MINUTOS;
    }
}

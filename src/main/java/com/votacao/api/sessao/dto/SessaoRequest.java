package com.votacao.api.sessao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Positive;

public record SessaoRequest(
        @JsonProperty("duracao_minutos") @Positive(message = "duracao_minutos deve ser positiva") Integer duracaoMinutos) {

    public static final int DURACAO_PADRAO_MINUTOS = 1;

    public int duracaoMinutosOuPadrao() {
        return duracaoMinutos != null ? duracaoMinutos : DURACAO_PADRAO_MINUTOS;
    }
}

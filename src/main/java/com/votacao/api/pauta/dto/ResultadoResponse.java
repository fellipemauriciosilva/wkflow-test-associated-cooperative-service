package com.votacao.api.pauta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.votacao.domain.sessao.StatusSessao;
import java.time.LocalDateTime;

public record ResultadoResponse(
        @JsonProperty("votos_sim") long votosSim,
        @JsonProperty("votos_nao") long votosNao,
        @JsonProperty("status_sessao") StatusSessao statusSessao,
        @JsonProperty("total_votos") long totalVotos,
        LocalDateTime timestamp) {
}

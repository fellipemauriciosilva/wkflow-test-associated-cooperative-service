package com.votacao.api.voto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.votacao.domain.voto.TipoVoto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VotoRequest(
        @JsonProperty("id_associado")
        @NotNull(message = "id_associado é obrigatório")
        @Positive(message = "id_associado deve ser positivo")
        Long idAssociado,
        @NotNull(message = "voto é obrigatório") TipoVoto voto) {
}

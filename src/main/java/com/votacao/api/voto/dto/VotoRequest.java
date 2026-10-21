package com.votacao.api.voto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.votacao.domain.voto.TipoVoto;
import jakarta.validation.constraints.NotNull;

public record VotoRequest(
        @JsonProperty("id_associado") @NotNull(message = "id_associado é obrigatório") Long idAssociado,
        @NotNull(message = "voto é obrigatório") TipoVoto voto) {
}

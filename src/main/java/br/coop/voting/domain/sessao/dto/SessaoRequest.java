package br.coop.voting.domain.sessao.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SessaoRequest(
    @NotNull(message = "duracao_minutos não pode ser nulo")
    @Min(value = 1, message = "duracao_minutos deve ser no mínimo 1")
    @Max(value = 60, message = "duracao_minutos deve ser no máximo 60")
    Integer duracao_minutos
) {
}

package br.coop.voting.domain.voto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VotoRequestDTO(
    @NotBlank(message = "id_associado não pode estar vazio")
    String idAssociado,

    @NotBlank(message = "voto é obrigatório")
    @Pattern(regexp = "^(?i)(SIM|NAO)$", message = "voto deve ser SIM ou NAO")
    String voto
) {}

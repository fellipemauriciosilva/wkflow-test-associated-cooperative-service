package br.coop.voting.domain.voto.dto;

import java.time.LocalDateTime;

public record VotoResponseDTO(
    Long id,
    Long pautaId,
    String idAssociado,
    String voto,
    LocalDateTime createdAt
) {}

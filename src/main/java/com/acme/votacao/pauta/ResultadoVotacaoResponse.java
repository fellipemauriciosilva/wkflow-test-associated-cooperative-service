package com.acme.votacao.pauta;

public record ResultadoVotacaoResponse(Long pautaId, long totalSim, long totalNao, StatusVotacao status) {
}

package com.cooperativa.votacao.api.voto.dto;

import com.cooperativa.votacao.domain.voto.TipoVoto;

public record VotoRequest(String associado, TipoVoto voto) {
}

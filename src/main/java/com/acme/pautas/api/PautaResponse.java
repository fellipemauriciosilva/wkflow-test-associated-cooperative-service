package com.acme.pautas.api;

import com.acme.pautas.domain.Pauta;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PautaResponse(Long id, String titulo, String descricao) {

    static PautaResponse de(Pauta pauta) {
        return new PautaResponse(pauta.getId(), pauta.getTitulo(), pauta.getDescricao());
    }
}

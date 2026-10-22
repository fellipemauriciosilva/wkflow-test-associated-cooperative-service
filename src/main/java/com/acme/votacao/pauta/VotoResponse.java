package com.acme.votacao.pauta;

public record VotoResponse(Long id, Long pautaId, String associadoId, VotoValor voto) {

    public static VotoResponse from(Voto voto) {
        return new VotoResponse(voto.getId(), voto.getPautaId(), voto.getAssociadoId(), voto.getVoto());
    }
}

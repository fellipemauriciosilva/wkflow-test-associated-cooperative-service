package br.acme.pauta;

import java.time.Instant;

public record VotoResponse(Long id, Long pautaId, Long sessaoId, String associadoId, String opcao, Instant criadoEm) {

    static VotoResponse de(Voto voto) {
        return new VotoResponse(voto.getId(), voto.getPauta().getId(), voto.getSessao().getId(),
                voto.getAssociadoId(), voto.getOpcao().name(), voto.getCriadoEm());
    }
}

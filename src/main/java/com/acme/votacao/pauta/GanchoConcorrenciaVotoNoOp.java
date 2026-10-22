package com.acme.votacao.pauta;

import org.springframework.stereotype.Component;

@Component
public class GanchoConcorrenciaVotoNoOp implements GanchoConcorrenciaVoto {

    @Override
    public void antesDeSalvar(Long pautaId, String associadoId) {
        // no-op em produção
    }
}

package com.acme.votacao.pauta;

import org.springframework.stereotype.Component;

@Component
public class GanchoConcorrenciaSessaoNoOp implements GanchoConcorrenciaSessao {

    @Override
    public void aposObterLock(Long pautaId) {
        // no-op em produção
    }
}

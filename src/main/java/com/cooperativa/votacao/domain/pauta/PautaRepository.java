package com.cooperativa.votacao.domain.pauta;

import java.util.Optional;

public interface PautaRepository {

    Pauta salvar(Pauta pauta);

    Optional<Pauta> buscarPorId(Long id);
}

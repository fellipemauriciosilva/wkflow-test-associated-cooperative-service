package com.cooperativa.votacao.domain.voto;

import java.util.List;

public interface VotoRepository {

    Voto salvar(Voto voto);

    List<Voto> buscarPorPautaId(Long pautaId);
}

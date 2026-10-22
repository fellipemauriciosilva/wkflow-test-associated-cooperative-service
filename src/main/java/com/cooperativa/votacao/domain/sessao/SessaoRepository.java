package com.cooperativa.votacao.domain.sessao;

import java.util.List;

public interface SessaoRepository {

    Sessao salvar(Sessao sessao);

    List<Sessao> buscarPorPautaId(Long pautaId);
}

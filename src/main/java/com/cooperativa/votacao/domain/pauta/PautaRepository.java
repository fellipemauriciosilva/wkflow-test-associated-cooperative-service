package com.cooperativa.votacao.domain.pauta;

import java.util.Optional;

public interface PautaRepository {

    Pauta salvar(Pauta pauta);

    Optional<Pauta> buscarPorId(Long id);

    /**
     * Busca a pauta tomando lock exclusivo (PESSIMISTIC_WRITE) na linha, para uso dentro de
     * uma transacao que depois verifica/decide sobre sessao aberta — ver SessaoService.abrir
     * e docs/CONTEXT.md (Decisões) sobre a escolha de lock pessimista em vez de @Version.
     */
    Optional<Pauta> buscarPorIdComLock(Long id);
}

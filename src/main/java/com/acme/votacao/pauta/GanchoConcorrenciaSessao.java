package com.acme.votacao.pauta;

/**
 * Ponto de extensão chamado por {@link SessaoService#abrir} logo após obter o lock pessimista
 * na pauta, antes de checar/inserir a sessão. Em produção é no-op; em teste, uma implementação
 * com barreira (latch) força a sobreposição real de duas transações concorrentes.
 */
public interface GanchoConcorrenciaSessao {

    void aposObterLock(Long pautaId);
}

package com.acme.votacao.pauta;

/**
 * Ponto de extensão chamado por {@link VotoTransacional#registrar} antes do lock pessimista
 * na pauta (e, portanto, antes das checagens de sessão/duplicado e do insert). Em produção é
 * no-op; em teste, uma implementação com barreira retém as duas chamadas concorrentes até
 * ambas chegarem, soltando-as juntas para disputar o lock de fato — exercitando tanto a
 * serialização pelo lock quanto, como rede de segurança, a constraint UNIQUE
 * (pauta_id, associado_id) do banco.
 */
public interface GanchoConcorrenciaVoto {

    void antesDeSalvar(Long pautaId, String associadoId);
}

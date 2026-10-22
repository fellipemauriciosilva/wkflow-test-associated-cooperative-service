package com.acme.votacao.pauta;

/**
 * Ponto de extensão chamado por {@link VotoService#votar} depois da checagem otimista de
 * duplicado e antes do insert. Em produção é no-op; em teste, uma implementação com barreira
 * força duas chamadas concorrentes a chegar ao insert ao mesmo tempo, exercitando de fato a
 * constraint UNIQUE (pauta_id, associado_id) do banco, não só a checagem otimista.
 */
public interface GanchoConcorrenciaVoto {

    void antesDeSalvar(Long pautaId, String associadoId);
}

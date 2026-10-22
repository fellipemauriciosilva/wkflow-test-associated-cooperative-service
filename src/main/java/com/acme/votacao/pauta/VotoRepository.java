package com.acme.votacao.pauta;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VotoRepository extends JpaRepository<Voto, Long> {

    boolean existsByPautaIdAndAssociadoId(Long pautaId, String associadoId);

    long countByPautaIdAndVoto(Long pautaId, VotoValor voto);

    long countByPautaId(Long pautaId);
}

package com.votacao.domain.voto;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VotoRepository extends JpaRepository<Voto, Long> {

    boolean existsByPautaIdAndIdAssociado(Long pautaId, Long idAssociado);
}

package com.cooperativa.votacao.infrastructure.persistence;

import com.cooperativa.votacao.domain.voto.Voto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VotoJpaRepository extends JpaRepository<Voto, Long> {

    List<Voto> findByPautaId(Long pautaId);
}

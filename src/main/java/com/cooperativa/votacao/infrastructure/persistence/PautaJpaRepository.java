package com.cooperativa.votacao.infrastructure.persistence;

import com.cooperativa.votacao.domain.pauta.Pauta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PautaJpaRepository extends JpaRepository<Pauta, Long> {
}

package com.acme.pautas.repository;

import com.acme.pautas.domain.Sessao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface SessaoRepository extends JpaRepository<Sessao, Long> {

    boolean existsByPautaIdAndFimAfter(Long pautaId, Instant agora);

    long countByPautaId(Long pautaId);
}

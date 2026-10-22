package com.acme.votacao.pauta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface SessaoRepository extends JpaRepository<Sessao, Long> {

    Optional<Sessao> findFirstByPautaIdAndFimAfter(Long pautaId, Instant agora);
}

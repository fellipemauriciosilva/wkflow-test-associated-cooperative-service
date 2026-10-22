package com.cooperativa.votacao.infrastructure.persistence;

import com.cooperativa.votacao.domain.sessao.Sessao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoJpaRepository extends JpaRepository<Sessao, Long> {

    List<Sessao> findByPautaId(Long pautaId);
}

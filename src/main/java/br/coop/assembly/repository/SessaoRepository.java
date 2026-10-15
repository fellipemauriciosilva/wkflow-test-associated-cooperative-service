package br.coop.assembly.repository;

import br.coop.assembly.entity.Sessao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SessaoRepository extends JpaRepository<Sessao, Long> {
    Optional<Sessao> findByPautaId(Long pautaId);

    @Query("SELECT s FROM Sessao s WHERE s.pautaId = ?1 AND s.status = 'ATIVA' AND s.dataFim > ?2")
    Optional<Sessao> findActiveSessaoForPauta(Long pautaId, LocalDateTime now);
}

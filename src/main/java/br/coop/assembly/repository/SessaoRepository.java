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

    @Query("SELECT s FROM Sessao s WHERE s.pautaId = :pautaId AND s.status = 'ATIVA' AND s.dataFim > :now")
    Optional<Sessao> findActiveSessaoForPauta(@Param("pautaId") Long pautaId, @Param("now") LocalDateTime now);
}

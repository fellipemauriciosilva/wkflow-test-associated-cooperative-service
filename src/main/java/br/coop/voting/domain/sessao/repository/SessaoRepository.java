package br.coop.voting.domain.sessao.repository;

import br.coop.voting.domain.sessao.entity.Sessao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessaoRepository extends JpaRepository<Sessao, String> {

    Optional<Sessao> findByPautaIdAndAtivaTrue(Long pautaId);

    @Query("SELECT s FROM Sessao s WHERE s.ativa = true AND s.dataFechamento IS NULL")
    List<Sessao> findByAtivaAndDataFechamentoNull();

    @Query("SELECT s FROM Sessao s WHERE s.pautaId = :pautaId AND s.ativa = false ORDER BY s.dataFechamento DESC LIMIT 1")
    Optional<Sessao> findLatestClosedByPautaId(@Param("pautaId") Long pautaId);

    @Query("SELECT s FROM Sessao s WHERE s.ativa = true AND s.dataFechamento IS NULL")
    List<Sessao> findAllActiveSessions();
}

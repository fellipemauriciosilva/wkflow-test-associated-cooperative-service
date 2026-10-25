package br.acme.pauta;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessaoRepository extends JpaRepository<Sessao, Long> {

    @Query("select count(s) from Sessao s where s.pauta.id = :pautaId and s.inicio <= :agora and s.fim > :agora")
    long countAbertasEm(@Param("pautaId") Long pautaId, @Param("agora") Instant agora);

    List<Sessao> findByPautaId(Long pautaId);
}

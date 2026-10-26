package br.acme.wkflow.sessao;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoRepository extends JpaRepository<Sessao, Long> {

    boolean existsByPautaIdAndFimGreaterThanEqual(Long pautaId, Instant agora);
}

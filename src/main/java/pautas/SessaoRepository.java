package pautas;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoRepository extends JpaRepository<Sessao, Long> {

    boolean existsByPautaId(Long pautaId);
}

package br.acme.pauta;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VotoRepository extends JpaRepository<Voto, Long> {

    boolean existsByPautaIdAndAssociadoId(Long pautaId, String associadoId);

    long countByPautaIdAndOpcao(Long pautaId, OpcaoVoto opcao);

    List<Voto> findByPautaId(Long pautaId);
}

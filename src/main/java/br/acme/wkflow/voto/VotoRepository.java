package br.acme.wkflow.voto;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VotoRepository extends JpaRepository<Voto, Long> {

    long countByPautaIdAndValor(Long pautaId, ValorVoto valor);

    boolean existsByPautaIdAndAssociadoId(Long pautaId, String associadoId);
}

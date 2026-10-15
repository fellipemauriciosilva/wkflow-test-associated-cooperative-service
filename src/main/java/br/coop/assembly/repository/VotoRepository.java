package br.coop.assembly.repository;

import br.coop.assembly.entity.Voto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VotoRepository extends JpaRepository<Voto, Long> {
    List<Voto> findByPautaId(Long pautaId);

    Optional<Voto> findByPautaIdAndAssociadoId(Long pautaId, String associadoId);

    int countByPautaId(Long pautaId);

    void deleteByPautaId(Long pautaId);
}

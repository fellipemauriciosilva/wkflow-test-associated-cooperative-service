package br.coop.assembly.repository;

import br.coop.assembly.entity.Voto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VotoRepository extends JpaRepository<Voto, Long> {
    Optional<Voto> findByPautaIdAndAssociadoId(Long pautaId, String associadoId);

    List<Voto> findByPautaId(Long pautaId);

    long countByPautaIdAndVoto(Long pautaId, Voto.VotoEnum voto);
}

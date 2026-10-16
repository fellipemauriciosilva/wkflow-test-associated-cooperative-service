package br.coop.voting.domain.voto.repository;

import br.coop.voting.domain.voto.entity.Voto;
import br.coop.voting.domain.voto.entity.VotoEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VotoRepository extends JpaRepository<Voto, Long> {
    Optional<Voto> findByPautaIdAndIdAssociado(Long pautaId, String idAssociado);

    Long countByPautaIdAndVoto(Long pautaId, VotoEnum voto);
}

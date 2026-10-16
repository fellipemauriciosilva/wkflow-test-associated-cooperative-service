package br.coop.voting.domain.pauta.repository;

import br.coop.voting.domain.pauta.entity.Pauta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PautaRepository extends JpaRepository<Pauta, Long> {

    List<Pauta> findByTituloContainingIgnoreCase(String titulo);

    @Query("SELECT p FROM Pauta p WHERE LOWER(p.titulo) = LOWER(:titulo)")
    Optional<Pauta> findByTituloExact(@Param("titulo") String titulo);
}

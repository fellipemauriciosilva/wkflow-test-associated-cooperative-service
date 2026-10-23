package br.coop.acme.pauta.repository;

import br.coop.acme.pauta.entity.Pauta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PautaRepository extends JpaRepository<Pauta, Long> {
}

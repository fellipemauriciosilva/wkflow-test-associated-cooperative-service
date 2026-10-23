package br.coop.assembleia.pauta;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PautaRepository extends JpaRepository<Pauta, UUID> {
}

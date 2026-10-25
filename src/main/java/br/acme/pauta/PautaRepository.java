package br.acme.pauta;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PautaRepository extends JpaRepository<Pauta, Long> {
}

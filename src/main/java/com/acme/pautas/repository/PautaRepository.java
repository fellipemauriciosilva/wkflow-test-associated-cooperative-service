package com.acme.pautas.repository;

import com.acme.pautas.domain.Pauta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface PautaRepository extends JpaRepository<Pauta, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Pauta> findWithLockById(Long id);
}

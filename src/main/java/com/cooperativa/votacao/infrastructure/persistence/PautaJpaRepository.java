package com.cooperativa.votacao.infrastructure.persistence;

import com.cooperativa.votacao.domain.pauta.Pauta;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PautaJpaRepository extends JpaRepository<Pauta, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pauta p where p.id = :id")
    Optional<Pauta> buscarPorIdComLock(@Param("id") Long id);
}

package br.acme.pauta;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PautaRepository extends JpaRepository<Pauta, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pauta p where p.id = :id")
    Optional<Pauta> findComLockPorId(@Param("id") Long id);
}

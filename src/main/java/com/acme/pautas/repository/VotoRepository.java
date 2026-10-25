package com.acme.pautas.repository;

import com.acme.pautas.domain.Voto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VotoRepository extends JpaRepository<Voto, Long> {

    boolean existsByPautaIdAndAssociadoId(Long pautaId, String associadoId);

    long countByPautaId(Long pautaId);

    @Query("select v.opcao, count(v) from Voto v where v.pautaId = :pautaId group by v.opcao")
    List<Object[]> contarPorOpcao(@Param("pautaId") Long pautaId);
}

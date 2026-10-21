package com.votacao.domain.voto;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VotoRepository extends JpaRepository<Voto, Long> {

    boolean existsByPautaIdAndIdAssociado(Long pautaId, Long idAssociado);

    @Query("SELECT v.tipo AS tipo, COUNT(v) AS total FROM Voto v WHERE v.pauta.id = :pautaId GROUP BY v.tipo")
    List<ContagemPorTipo> contarPorTipo(@Param("pautaId") Long pautaId);

    interface ContagemPorTipo {
        TipoVoto getTipo();

        long getTotal();
    }
}

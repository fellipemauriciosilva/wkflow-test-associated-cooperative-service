package com.cooperativa.votacao.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cooperativa.votacao.domain.pauta.Pauta;
import com.cooperativa.votacao.domain.voto.TipoVoto;
import com.cooperativa.votacao.domain.voto.Voto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Prova que a constraint uk_voto_associado_pauta existe NO BANCO (nao so em memoria no
 * service): saveAndFlush de segundo voto do mesmo associado na mesma pauta deve falhar
 * mesmo que o check em VotoService.registrar fosse removido ou tivesse um bug.
 */
@DataJpaTest
class VotoJpaRepositoryTest {

    @Autowired
    private PautaJpaRepository pautaJpaRepository;

    @Autowired
    private VotoJpaRepository votoJpaRepository;

    @Test
    @DisplayName("saveAndFlush de voto duplicado (mesmo associado, mesma pauta) lanca DataIntegrityViolationException")
    void saveAndFlush_comVotoDuplicado_lancaExcecao() {
        var pauta = pautaJpaRepository.saveAndFlush(new Pauta("Eleicao de diretoria", null));
        votoJpaRepository.saveAndFlush(new Voto(pauta, "associado-A", TipoVoto.SIM));

        assertThatThrownBy(() ->
                        votoJpaRepository.saveAndFlush(new Voto(pauta, "associado-A", TipoVoto.NAO)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("mesmo associado em pautas diferentes nao viola a constraint")
    void saveAndFlush_comMesmoAssociadoEmPautasDiferentes_permiteAmbos() {
        var pautaA = pautaJpaRepository.saveAndFlush(new Pauta("Eleicao de diretoria", null));
        var pautaB = pautaJpaRepository.saveAndFlush(new Pauta("Reforma do estatuto", null));

        votoJpaRepository.saveAndFlush(new Voto(pautaA, "associado-A", TipoVoto.SIM));
        votoJpaRepository.saveAndFlush(new Voto(pautaB, "associado-A", TipoVoto.NAO));
    }
}

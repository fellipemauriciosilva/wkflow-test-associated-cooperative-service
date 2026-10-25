package br.acme.pauta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class VotoServiceTest {

    @Test
    void violacaoDaRestricaoUnicaDeVotoEReconhecida() {
        DataIntegrityViolationException erro = new DataIntegrityViolationException("could not execute statement",
                new RuntimeException("Unique index or primary key violation: \"UK_VOTO_PAUTA_ASSOCIADO_INDEX_2 ON PUBLIC.VOTO(PAUTA_ID, ASSOCIADO_ID)\""));

        assertThat(VotoService.violaRestricaoUnica(erro)).isTrue();
    }

    @Test
    void outraViolacaoDeIntegridadeNaoEReconhecidaComoVotoDuplicado() {
        DataIntegrityViolationException erro = new DataIntegrityViolationException("could not execute statement",
                new RuntimeException("Referential integrity constraint violation: \"FK_VOTO_SESSAO: PUBLIC.VOTO FOREIGN KEY(SESSAO_ID)\""));

        assertThat(VotoService.violaRestricaoUnica(erro)).isFalse();
    }
}

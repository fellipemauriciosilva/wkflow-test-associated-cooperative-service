package br.acme.pauta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class VotoServiceTest {

    @Test
    void violacaoUnicaDoH2ReconhecidaPorSqlState() {
        SQLException causa = new SQLException(
                "Unique index or primary key violation: \"UK_VOTO_PAUTA_ASSOCIADO_INDEX_2 ON PUBLIC.VOTO(PAUTA_ID, ASSOCIADO_ID)\"",
                "23505");
        DataIntegrityViolationException erro = new DataIntegrityViolationException("could not execute statement", causa);

        assertThat(VotoService.violaRestricaoUnica(erro)).isTrue();
    }

    @Test
    void violacaoUnicaNoFormatoPostgreSqlReconhecidaPorSqlState() {
        SQLException causa = new SQLException(
                "ERROR: duplicate key value violates unique constraint \"uk_voto_pauta_associado\"",
                "23505");
        DataIntegrityViolationException erro = new DataIntegrityViolationException("could not execute statement", causa);

        assertThat(VotoService.violaRestricaoUnica(erro)).isTrue();
    }

    @Test
    void violacaoDeChaveEstrangeiraNaoEReconhecidaComoVotoDuplicado() {
        SQLException causa = new SQLException(
                "ERROR: insert or update on table \"voto\" violates foreign key constraint \"fk_voto_sessao\"",
                "23503");
        DataIntegrityViolationException erro = new DataIntegrityViolationException("could not execute statement", causa);

        assertThat(VotoService.violaRestricaoUnica(erro)).isFalse();
    }

    @Test
    void constraintUnicaNoSaveAndFlushViraVotoDuplicadoMesmoComExistsByFalso() {
        PautaRepository pautas = mock(PautaRepository.class);
        SessaoRepository sessoes = mock(SessaoRepository.class);
        VotoRepository votos = mock(VotoRepository.class);
        Instant agora = Instant.parse("2026-01-01T10:00:00Z");
        Pauta pauta = new Pauta("Pauta", null);
        Sessao sessao = new Sessao(pauta, agora.minusSeconds(60), agora.plusSeconds(60));
        when(pautas.findComLockPorId(1L)).thenReturn(Optional.of(pauta));
        when(sessoes.findAbertaEm(1L, agora)).thenReturn(Optional.of(sessao));
        when(votos.existsByPautaIdAndAssociadoId(1L, "a1")).thenReturn(false);
        SQLException causa = new SQLException(
                "Unique index or primary key violation: \"UK_VOTO_PAUTA_ASSOCIADO_INDEX_2 ON PUBLIC.VOTO(PAUTA_ID, ASSOCIADO_ID)\"",
                "23505");
        when(votos.saveAndFlush(any(Voto.class))).thenThrow(
                new DataIntegrityViolationException("could not execute statement", causa));
        VotoService service = new VotoService(pautas, sessoes, votos, Clock.fixed(agora, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.registrar(1L, new VotoRequest("a1", "SIM")))
                .isInstanceOf(VotoDuplicadoException.class)
                .hasMessage("Associado ja votou na pauta: 1");
    }
}

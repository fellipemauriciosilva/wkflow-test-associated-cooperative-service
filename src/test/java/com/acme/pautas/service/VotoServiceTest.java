package com.acme.pautas.service;

import com.acme.pautas.domain.OpcaoVoto;
import com.acme.pautas.domain.Pauta;
import com.acme.pautas.domain.Voto;
import com.acme.pautas.repository.PautaRepository;
import com.acme.pautas.repository.SessaoRepository;
import com.acme.pautas.repository.VotoRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VotoServiceTest {

    private final PautaRepository pautas = mock(PautaRepository.class);
    private final SessaoRepository sessoes = mock(SessaoRepository.class);
    private final VotoRepository votos = mock(VotoRepository.class);
    private final Clock clock = Clock.systemUTC();
    private final VotoService servico = new VotoService(pautas, sessoes, votos, clock);

    private void pautaComSessaoAberta() {
        when(pautas.findWithLockById(anyLong())).thenReturn(Optional.of(new Pauta("p", null)));
        when(sessoes.existsByPautaIdAndFimAfter(anyLong(), any(Instant.class))).thenReturn(true);
        when(votos.existsByPautaIdAndAssociadoId(anyLong(), anyString())).thenReturn(false);
    }

    private static DataIntegrityViolationException violacao(String constraint) {
        ConstraintViolationException causa = new ConstraintViolationException(
                "violação", new SQLException("violação"), constraint);
        return new DataIntegrityViolationException("could not execute statement", causa);
    }

    @Test
    void violacaoDaConstraintUnicaDeVotoViraDuplicidade() {
        pautaComSessaoAberta();
        when(votos.saveAndFlush(any(Voto.class))).thenThrow(violacao("UK_VOTO_PAUTA_ASSOCIADO"));

        assertThrows(VotoDuplicadoException.class,
                () -> servico.registrar(1L, "a1", OpcaoVoto.SIM));
    }

    @Test
    void violacaoDeForeignKeyNaoViraDuplicidade() {
        pautaComSessaoAberta();
        when(votos.saveAndFlush(any(Voto.class))).thenThrow(violacao("FK_VOTO_PAUTA"));

        assertThrows(DataIntegrityViolationException.class,
                () -> servico.registrar(1L, "a1", OpcaoVoto.SIM));
    }

    @Test
    void violacaoNotNullNaoViraDuplicidade() {
        pautaComSessaoAberta();
        when(votos.saveAndFlush(any(Voto.class))).thenThrow(new DataIntegrityViolationException(
                "NULL not allowed for column \"OPCAO\""));

        assertThrows(DataIntegrityViolationException.class,
                () -> servico.registrar(1L, "a1", OpcaoVoto.SIM));
    }

    @Test
    void associadoIdComEspacosQueCabeNos255CaracteresEhAceito() {
        pautaComSessaoAberta();
        when(votos.saveAndFlush(any(Voto.class))).thenAnswer(chamada -> chamada.getArgument(0));
        String comEspacos = "  " + "a".repeat(255) + "  ";

        assertDoesNotThrow(() -> servico.registrar(1L, comEspacos, OpcaoVoto.SIM));
    }

    @Test
    void associadoIdComEspacosQueUltrapassa255CaracteresAposStripEhRejeitado() {
        pautaComSessaoAberta();
        String comEspacos = "  " + "a".repeat(256) + "  ";

        assertThrows(AssociadoInvalidoException.class,
                () -> servico.registrar(1L, comEspacos, OpcaoVoto.SIM));
        verify(votos, never()).saveAndFlush(any(Voto.class));
    }

    @Test
    void tamanhoContaCodePointsNaoUnidadesUtf16() {
        pautaComSessaoAberta();
        when(votos.saveAndFlush(any(Voto.class))).thenAnswer(chamada -> chamada.getArgument(0));
        String emojis = "😀".repeat(255);

        assertDoesNotThrow(() -> servico.registrar(1L, emojis, OpcaoVoto.SIM));
        assertThrows(AssociadoInvalidoException.class,
                () -> servico.registrar(1L, "😀".repeat(256), OpcaoVoto.SIM));
    }
}

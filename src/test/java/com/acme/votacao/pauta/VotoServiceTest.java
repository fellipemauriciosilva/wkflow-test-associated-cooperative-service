package com.acme.votacao.pauta;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VotoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-01-01T10:00:00Z");
    private static final Long PAUTA_ID = 1L;
    private static final String ASSOCIADO_ID = "associado-1";

    @Mock
    private PautaRepository pautaRepository;

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private VotoRepository votoRepository;

    private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);

    private VotoService sut;

    @BeforeEach
    void setUp() {
        sut = new VotoService(pautaRepository, sessaoRepository, votoRepository, clock, new GanchoConcorrenciaVotoNoOp());
    }

    @Test
    @DisplayName("deve registrar voto quando sessão está aberta e associado não votou")
    void votar_sessaoAbertaESemVotoPrevio_registraVoto() {
        when(pautaRepository.existsById(PAUTA_ID)).thenReturn(true);
        Sessao sessaoAberta = new Sessao(PAUTA_ID, AGORA.minusSeconds(30), AGORA.plusSeconds(30), 1);
        when(sessaoRepository.findFirstByPautaIdAndFimAfter(PAUTA_ID, AGORA)).thenReturn(Optional.of(sessaoAberta));
        when(votoRepository.existsByPautaIdAndAssociadoId(PAUTA_ID, ASSOCIADO_ID)).thenReturn(false);
        when(votoRepository.save(any(Voto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Voto resultado = sut.votar(PAUTA_ID, new VotoRequest(ASSOCIADO_ID, VotoValor.SIM));

        assertThat(resultado.getPautaId()).isEqualTo(PAUTA_ID);
        assertThat(resultado.getAssociadoId()).isEqualTo(ASSOCIADO_ID);
        assertThat(resultado.getVoto()).isEqualTo(VotoValor.SIM);
    }

    @Test
    @DisplayName("deve lançar PautaNaoEncontradaException quando pauta não existe")
    void votar_pautaInexistente_lancaExcecao() {
        when(pautaRepository.existsById(PAUTA_ID)).thenReturn(false);

        assertThatThrownBy(() -> sut.votar(PAUTA_ID, new VotoRequest(ASSOCIADO_ID, VotoValor.SIM)))
                .isInstanceOf(PautaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("deve lançar SessaoNaoAbertaException quando não há sessão aberta para a pauta")
    void votar_semSessaoAberta_lancaExcecao() {
        when(pautaRepository.existsById(PAUTA_ID)).thenReturn(true);
        when(sessaoRepository.findFirstByPautaIdAndFimAfter(PAUTA_ID, AGORA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.votar(PAUTA_ID, new VotoRequest(ASSOCIADO_ID, VotoValor.SIM)))
                .isInstanceOf(SessaoNaoAbertaException.class);
    }

    @Test
    @DisplayName("deve lançar VotoDuplicadoException quando associado já votou (checagem otimista)")
    void votar_associadoJaVotou_lancaExcecao() {
        when(pautaRepository.existsById(PAUTA_ID)).thenReturn(true);
        Sessao sessaoAberta = new Sessao(PAUTA_ID, AGORA.minusSeconds(30), AGORA.plusSeconds(30), 1);
        when(sessaoRepository.findFirstByPautaIdAndFimAfter(PAUTA_ID, AGORA)).thenReturn(Optional.of(sessaoAberta));
        when(votoRepository.existsByPautaIdAndAssociadoId(PAUTA_ID, ASSOCIADO_ID)).thenReturn(true);

        assertThatThrownBy(() -> sut.votar(PAUTA_ID, new VotoRequest(ASSOCIADO_ID, VotoValor.SIM)))
                .isInstanceOf(VotoDuplicadoException.class);
    }

    @Test
    @DisplayName("deve lançar VotoDuplicadoException quando a constraint de unicidade do banco rejeita o insert")
    void votar_constraintDeUnicidadeViolada_lancaVotoDuplicadoException() {
        when(pautaRepository.existsById(PAUTA_ID)).thenReturn(true);
        Sessao sessaoAberta = new Sessao(PAUTA_ID, AGORA.minusSeconds(30), AGORA.plusSeconds(30), 1);
        when(sessaoRepository.findFirstByPautaIdAndFimAfter(PAUTA_ID, AGORA)).thenReturn(Optional.of(sessaoAberta));
        when(votoRepository.existsByPautaIdAndAssociadoId(PAUTA_ID, ASSOCIADO_ID)).thenReturn(false);
        when(votoRepository.save(any(Voto.class))).thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> sut.votar(PAUTA_ID, new VotoRequest(ASSOCIADO_ID, VotoValor.SIM)))
                .isInstanceOf(VotoDuplicadoException.class);
    }
}

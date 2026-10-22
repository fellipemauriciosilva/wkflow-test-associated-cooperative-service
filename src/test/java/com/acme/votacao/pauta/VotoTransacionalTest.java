package com.acme.votacao.pauta;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VotoTransacionalTest {

    private static final Instant AGORA = Instant.parse("2026-01-01T10:00:00Z");
    private static final Long PAUTA_ID = 1L;
    private static final String ASSOCIADO_ID = "associado-1";

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private VotoRepository votoRepository;

    @Mock
    private EntityManager entityManager;

    private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);

    private VotoTransacional sut;

    @BeforeEach
    void setUp() {
        sut = new VotoTransacional(sessaoRepository, votoRepository, entityManager, clock, new GanchoConcorrenciaVotoNoOp());
    }

    @Test
    @DisplayName("deve registrar voto quando sessão está aberta e associado não votou")
    void registrar_sessaoAbertaESemVotoPrevio_registraVoto() {
        Sessao sessaoAberta = new Sessao(PAUTA_ID, AGORA.minusSeconds(30), AGORA.plusSeconds(30), 1);
        when(sessaoRepository.findFirstByPautaIdAndFimAfter(PAUTA_ID, AGORA)).thenReturn(Optional.of(sessaoAberta));
        when(votoRepository.existsByPautaIdAndAssociadoId(PAUTA_ID, ASSOCIADO_ID)).thenReturn(false);
        when(votoRepository.save(any(Voto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Voto resultado = sut.registrar(PAUTA_ID, ASSOCIADO_ID, VotoValor.SIM);

        assertThat(resultado.getPautaId()).isEqualTo(PAUTA_ID);
        assertThat(resultado.getAssociadoId()).isEqualTo(ASSOCIADO_ID);
        assertThat(resultado.getVoto()).isEqualTo(VotoValor.SIM);
    }

    @Test
    @DisplayName("deve lançar SessaoNaoAbertaException quando não há sessão para a pauta")
    void registrar_semSessao_lancaSessaoNaoAbertaException() {
        when(sessaoRepository.findFirstByPautaIdAndFimAfter(PAUTA_ID, AGORA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.registrar(PAUTA_ID, ASSOCIADO_ID, VotoValor.SIM))
                .isInstanceOf(SessaoNaoAbertaException.class);

        verify(votoRepository, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar SessaoNaoAbertaException quando agora é igual ao fim da sessão (fronteira fechada)")
    void registrar_agoraIgualAoFim_lancaSessaoNaoAbertaException() {
        // findFirstByPautaIdAndFimAfter usa fim > agora estrito; agora == fim não é "after".
        when(sessaoRepository.findFirstByPautaIdAndFimAfter(PAUTA_ID, AGORA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.registrar(PAUTA_ID, ASSOCIADO_ID, VotoValor.SIM))
                .isInstanceOf(SessaoNaoAbertaException.class);

        verify(votoRepository, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar VotoDuplicadoException quando associado já votou (checagem otimista)")
    void registrar_associadoJaVotou_lancaVotoDuplicadoException() {
        Sessao sessaoAberta = new Sessao(PAUTA_ID, AGORA.minusSeconds(30), AGORA.plusSeconds(30), 1);
        when(sessaoRepository.findFirstByPautaIdAndFimAfter(PAUTA_ID, AGORA)).thenReturn(Optional.of(sessaoAberta));
        when(votoRepository.existsByPautaIdAndAssociadoId(PAUTA_ID, ASSOCIADO_ID)).thenReturn(true);

        assertThatThrownBy(() -> sut.registrar(PAUTA_ID, ASSOCIADO_ID, VotoValor.SIM))
                .isInstanceOf(VotoDuplicadoException.class);

        verify(votoRepository, never()).save(any());
    }
}

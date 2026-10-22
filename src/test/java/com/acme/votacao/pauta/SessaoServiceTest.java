package com.acme.votacao.pauta;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessaoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-01-01T10:00:00Z");
    private static final Long PAUTA_ID = 1L;

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private EntityManager entityManager;

    private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);

    private SessaoService sut;

    @BeforeEach
    void setUp() {
        sut = new SessaoService(sessaoRepository, entityManager, clock, new GanchoConcorrenciaSessaoNoOp());
    }

    @Test
    @DisplayName("deve abrir sessão com duração padrão de 1 minuto quando não informada")
    void abrir_semDuracao_usaDuracaoPadraoDeUmMinuto() {
        Pauta pauta = new Pauta("Reforma do estatuto", null);
        when(entityManager.find(Pauta.class, PAUTA_ID, LockModeType.PESSIMISTIC_WRITE)).thenReturn(pauta);
        when(sessaoRepository.findFirstByPautaIdAndFimAfter(PAUTA_ID, AGORA)).thenReturn(Optional.empty());
        when(sessaoRepository.save(any(Sessao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sessao resultado = sut.abrir(PAUTA_ID, new SessaoRequest(null));

        assertThat(resultado.getDuracaoMinutos()).isEqualTo(1);
        assertThat(resultado.getInicio()).isEqualTo(AGORA);
        assertThat(resultado.getFim()).isEqualTo(AGORA.plusSeconds(60));
        assertThat(resultado.getPautaId()).isEqualTo(PAUTA_ID);
    }

    @Test
    @DisplayName("deve abrir sessão com duração informada")
    void abrir_comDuracao_calculaFimConformeInformado() {
        Pauta pauta = new Pauta("Reforma do estatuto", null);
        when(entityManager.find(Pauta.class, PAUTA_ID, LockModeType.PESSIMISTIC_WRITE)).thenReturn(pauta);
        when(sessaoRepository.findFirstByPautaIdAndFimAfter(PAUTA_ID, AGORA)).thenReturn(Optional.empty());
        ArgumentCaptor<Sessao> captor = ArgumentCaptor.forClass(Sessao.class);
        when(sessaoRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        sut.abrir(PAUTA_ID, new SessaoRequest(5));

        assertThat(captor.getValue().getDuracaoMinutos()).isEqualTo(5);
        assertThat(captor.getValue().getFim()).isEqualTo(AGORA.plusSeconds(5 * 60));
    }

    @Test
    @DisplayName("deve lançar PautaNaoEncontradaException quando pauta não existe")
    void abrir_pautaInexistente_lancaExcecao() {
        when(entityManager.find(Pauta.class, PAUTA_ID, LockModeType.PESSIMISTIC_WRITE)).thenReturn(null);

        assertThatThrownBy(() -> sut.abrir(PAUTA_ID, new SessaoRequest(null)))
                .isInstanceOf(PautaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("deve lançar SessaoJaAbertaException quando já existe sessão aberta para a pauta")
    void abrir_sessaoJaAberta_lancaExcecao() {
        Pauta pauta = new Pauta("Reforma do estatuto", null);
        Sessao sessaoExistente = new Sessao(PAUTA_ID, AGORA.minusSeconds(30), AGORA.plusSeconds(30), 1);
        when(entityManager.find(Pauta.class, PAUTA_ID, LockModeType.PESSIMISTIC_WRITE)).thenReturn(pauta);
        when(sessaoRepository.findFirstByPautaIdAndFimAfter(PAUTA_ID, AGORA)).thenReturn(Optional.of(sessaoExistente));

        assertThatThrownBy(() -> sut.abrir(PAUTA_ID, new SessaoRequest(null)))
                .isInstanceOf(SessaoJaAbertaException.class);
    }
}

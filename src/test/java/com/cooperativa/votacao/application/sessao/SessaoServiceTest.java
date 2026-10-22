package com.cooperativa.votacao.application.sessao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.cooperativa.votacao.domain.pauta.Pauta;
import com.cooperativa.votacao.domain.pauta.PautaNaoEncontradaException;
import com.cooperativa.votacao.domain.pauta.PautaRepository;
import com.cooperativa.votacao.domain.sessao.Sessao;
import com.cooperativa.votacao.domain.sessao.SessaoInvalidaException;
import com.cooperativa.votacao.domain.sessao.SessaoJaAbertaException;
import com.cooperativa.votacao.domain.sessao.SessaoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessaoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-22T10:00:00Z");

    @Mock
    private PautaRepository pautaRepository;

    @Mock
    private SessaoRepository sessaoRepository;

    private SessaoService sut;
    private Pauta pauta;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(AGORA, ZoneOffset.UTC);
        sut = new SessaoService(pautaRepository, sessaoRepository, clock);
        pauta = new Pauta("Eleicao de diretoria", null);
    }

    @Test
    @DisplayName("abre sessao com duracao informada")
    void abrir_comDuracaoInformada_abreSessaoComDuracaoInformada() {
        when(pautaRepository.buscarPorId(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.buscarPorPautaId(1L)).thenReturn(List.of());
        when(sessaoRepository.salvar(any(Sessao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        var sessao = sut.abrir(1L, 5);

        assertThat(sessao.getDuracaoMinutos()).isEqualTo(5);
        assertThat(sessao.getAbertura()).isEqualTo(AGORA);
        assertThat(sessao.getFim()).isEqualTo(AGORA.plusSeconds(300));
    }

    @Test
    @DisplayName("duracao nula assume 1 minuto")
    void abrir_comDuracaoNula_assumeUmMinuto() {
        when(pautaRepository.buscarPorId(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.buscarPorPautaId(1L)).thenReturn(List.of());
        when(sessaoRepository.salvar(any(Sessao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        var sessao = sut.abrir(1L, null);

        assertThat(sessao.getDuracaoMinutos()).isEqualTo(Sessao.DURACAO_PADRAO_MINUTOS);
    }

    @Test
    @DisplayName("duracao menor ou igual a zero lanca SessaoInvalidaException antes de consultar a pauta")
    void abrir_comDuracaoInvalida_lancaExcecaoSemConsultarPauta() {
        assertThatThrownBy(() -> sut.abrir(1L, 0)).isInstanceOf(SessaoInvalidaException.class);
        assertThatThrownBy(() -> sut.abrir(1L, -1)).isInstanceOf(SessaoInvalidaException.class);
    }

    @Test
    @DisplayName("pauta inexistente lanca PautaNaoEncontradaException")
    void abrir_comPautaInexistente_lancaExcecao() {
        when(pautaRepository.buscarPorId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.abrir(999L, 5)).isInstanceOf(PautaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("sessao ja aberta para a pauta lanca SessaoJaAbertaException")
    void abrir_comSessaoJaAberta_lancaExcecao() {
        var sessaoAberta = new Sessao(pauta, 10, AGORA.minusSeconds(60));
        when(pautaRepository.buscarPorId(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.buscarPorPautaId(1L)).thenReturn(List.of(sessaoAberta));

        assertThatThrownBy(() -> sut.abrir(1L, 5)).isInstanceOf(SessaoJaAbertaException.class);
    }

    @Test
    @DisplayName("sessao encerrada nao bloqueia nova abertura")
    void abrir_comSessaoEncerrada_abreNovaSessao() {
        var sessaoEncerrada = new Sessao(pauta, 1, AGORA.minusSeconds(120));
        when(pautaRepository.buscarPorId(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.buscarPorPautaId(1L)).thenReturn(List.of(sessaoEncerrada));
        when(sessaoRepository.salvar(any(Sessao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        var sessao = sut.abrir(1L, 5);

        assertThat(sessao.getDuracaoMinutos()).isEqualTo(5);
    }
}

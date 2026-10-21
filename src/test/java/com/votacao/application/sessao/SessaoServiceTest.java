package com.votacao.application.sessao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.votacao.application.pauta.PautaService;
import com.votacao.domain.pauta.Pauta;
import com.votacao.domain.pauta.PautaNotFoundException;
import com.votacao.domain.sessao.Sessao;
import com.votacao.domain.sessao.SessaoJaAbertaException;
import com.votacao.domain.sessao.SessaoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class SessaoServiceTest {

    private static final ZoneId ZONE = ZoneId.of("UTC");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-10-21T10:00:00Z");

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private PautaService pautaService;

    private Clock clock;
    private SessaoService sut;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_INSTANT, ZONE);
        sut = new SessaoService(sessaoRepository, pautaService, clock);
    }

    @Test
    @DisplayName("abrir com duração custom deve calcular fechamento corretamente")
    void abrir_comDuracaoCustom_calculaFechamento() {
        Pauta pauta = new Pauta("Título", "Descrição");
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.empty());
        when(sessaoRepository.save(any(Sessao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sessao resultado = sut.abrir(1L, 5);

        LocalDateTime aberturaEsperada = LocalDateTime.ofInstant(FIXED_INSTANT, ZONE);
        assertThat(resultado.getAbertura()).isEqualTo(aberturaEsperada);
        assertThat(resultado.getFechamento()).isEqualTo(aberturaEsperada.plusMinutes(5));
        assertThat(resultado.getDuracaoMinutos()).isEqualTo(5);
    }

    @Test
    @DisplayName("abrir com duração default (1) deve calcular fechamento com 1 minuto")
    void abrir_comDuracaoDefault_calculaFechamentoComUmMinuto() {
        Pauta pauta = new Pauta("Título", "Descrição");
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.empty());
        when(sessaoRepository.save(any(Sessao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sessao resultado = sut.abrir(1L, 1);

        LocalDateTime aberturaEsperada = LocalDateTime.ofInstant(FIXED_INSTANT, ZONE);
        assertThat(resultado.getFechamento()).isEqualTo(aberturaEsperada.plusMinutes(1));
    }

    @Test
    @DisplayName("abrir em pauta inexistente deve propagar PautaNotFoundException")
    void abrir_pautaInexistente_propagaExcecao() {
        when(pautaService.buscarPorId(99L)).thenThrow(new PautaNotFoundException(99L));

        assertThatThrownBy(() -> sut.abrir(99L, 1))
                .isInstanceOf(PautaNotFoundException.class);
    }

    @Test
    @DisplayName("abrir sessão duplicada (check prévio) deve lançar SessaoJaAbertaException")
    void abrir_sessaoJaExistente_lancaExcecao() {
        Pauta pauta = new Pauta("Título", "Descrição");
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        Sessao existente = new Sessao(pauta, LocalDateTime.now(), 1);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> sut.abrir(1L, 1))
                .isInstanceOf(SessaoJaAbertaException.class);
    }

    @Test
    @DisplayName("abrir com corrida detectada pelo BD (constraint única) deve lançar SessaoJaAbertaException")
    void abrir_constraintUnicaViolada_lancaExcecao() {
        Pauta pauta = new Pauta("Título", "Descrição");
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.empty());
        when(sessaoRepository.save(any(Sessao.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> sut.abrir(1L, 1))
                .isInstanceOf(SessaoJaAbertaException.class);
    }
}

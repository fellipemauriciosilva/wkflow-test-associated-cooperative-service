package com.votacao.application.pauta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.votacao.api.pauta.dto.ResultadoResponse;
import com.votacao.domain.pauta.Pauta;
import com.votacao.domain.pauta.PautaNotFoundException;
import com.votacao.domain.sessao.Sessao;
import com.votacao.domain.sessao.SessaoRepository;
import com.votacao.domain.sessao.StatusSessao;
import com.votacao.domain.voto.TipoVoto;
import com.votacao.domain.voto.VotoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResultadoServiceTest {

    private static final ZoneId ZONE = ZoneId.of("UTC");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-10-21T10:00:00Z");

    @Mock
    private PautaService pautaService;

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private VotoRepository votoRepository;

    private Clock clock;
    private ResultadoService sut;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_INSTANT, ZONE);
        sut = new ResultadoService(pautaService, sessaoRepository, votoRepository, clock);
    }

    private VotoRepository.ContagemPorTipo contagem(TipoVoto tipo, long total) {
        return new VotoRepository.ContagemPorTipo() {
            @Override
            public TipoVoto getTipo() {
                return tipo;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    @Test
    @DisplayName("apurar com sessão aberta deve retornar contagens exatas e status ABERTA")
    void apurar_sessaoAberta_retornaContagensEStatusAberta() {
        Pauta pauta = new Pauta("Título", "Descrição");
        LocalDateTime abertura = LocalDateTime.now(clock).minusMinutes(1);
        Sessao sessao = new Sessao(pauta, abertura, 5);
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessao));
        when(votoRepository.contarPorTipo(1L))
                .thenReturn(List.of(contagem(TipoVoto.SIM, 3L), contagem(TipoVoto.NAO, 2L)));

        ResultadoResponse resultado = sut.apurar(1L);

        assertThat(resultado.votosSim()).isEqualTo(3L);
        assertThat(resultado.votosNao()).isEqualTo(2L);
        assertThat(resultado.totalVotos()).isEqualTo(5L);
        assertThat(resultado.statusSessao()).isEqualTo(StatusSessao.ABERTA);
    }

    @Test
    @DisplayName("apurar com sessão exatamente no limite de fechamento deve retornar status EXPIRADA")
    void apurar_sessaoNoLimiteDoFechamento_retornaStatusExpirada() {
        Pauta pauta = new Pauta("Título", "Descrição");
        LocalDateTime abertura = LocalDateTime.now(clock).minusMinutes(5);
        Sessao sessao = new Sessao(pauta, abertura, 5);
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessao));
        when(votoRepository.contarPorTipo(1L)).thenReturn(List.of());

        ResultadoResponse resultado = sut.apurar(1L);

        assertThat(resultado.statusSessao()).isEqualTo(StatusSessao.EXPIRADA);
        assertThat(resultado.votosSim()).isZero();
        assertThat(resultado.votosNao()).isZero();
        assertThat(resultado.totalVotos()).isZero();
    }

    @Test
    @DisplayName("apurar com sessão expirada deve manter contagens anteriores ao fechamento")
    void apurar_sessaoExpirada_mantemContagensAnteriores() {
        Pauta pauta = new Pauta("Título", "Descrição");
        LocalDateTime abertura = LocalDateTime.now(clock).minusMinutes(10);
        Sessao sessao = new Sessao(pauta, abertura, 5);
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessao));
        when(votoRepository.contarPorTipo(1L))
                .thenReturn(List.of(contagem(TipoVoto.SIM, 3L), contagem(TipoVoto.NAO, 2L)));

        ResultadoResponse resultado = sut.apurar(1L);

        assertThat(resultado.statusSessao()).isEqualTo(StatusSessao.EXPIRADA);
        assertThat(resultado.votosSim()).isEqualTo(3L);
        assertThat(resultado.votosNao()).isEqualTo(2L);
        assertThat(resultado.totalVotos()).isEqualTo(5L);
    }

    @Test
    @DisplayName("apurar pauta sem sessão deve retornar status NAO_INICIADA e contagens zeradas")
    void apurar_pautaSemSessao_retornaNaoIniciadaEZerado() {
        Pauta pauta = new Pauta("Título", "Descrição");
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.empty());

        ResultadoResponse resultado = sut.apurar(1L);

        assertThat(resultado.statusSessao()).isEqualTo(StatusSessao.NAO_INICIADA);
        assertThat(resultado.votosSim()).isZero();
        assertThat(resultado.votosNao()).isZero();
        assertThat(resultado.totalVotos()).isZero();
    }

    @Test
    @DisplayName("apurar deve propagar PautaNotFoundException quando pauta não existe")
    void apurar_pautaInexistente_propagaExcecao() {
        when(pautaService.buscarPorId(999L))
                .thenThrow(new PautaNotFoundException(999L));

        assertThatThrownBy(() -> sut.apurar(999L))
                .isInstanceOf(PautaNotFoundException.class);
    }
}

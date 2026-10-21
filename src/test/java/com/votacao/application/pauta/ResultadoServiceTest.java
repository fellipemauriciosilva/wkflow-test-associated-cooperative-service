package com.votacao.application.pauta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.votacao.api.pauta.dto.ResultadoResponse;
import com.votacao.domain.pauta.Pauta;
import com.votacao.domain.pauta.PautaNotFoundException;
import com.votacao.domain.sessao.Sessao;
import com.votacao.domain.sessao.SessaoRepository;
import com.votacao.domain.sessao.StatusSessao;
import com.votacao.domain.voto.TipoVoto;
import com.votacao.domain.voto.VotoRepository;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

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
    @DisplayName("apurar com sessão aberta deve ocultar contagens (zeradas) e retornar status ABERTA, sem consultar votos")
    void apurar_sessaoAberta_ocultaContagensEStatusAberta() {
        Pauta pauta = new Pauta("Título", "Descrição");
        LocalDateTime abertura = LocalDateTime.now(clock).minusMinutes(1);
        Sessao sessao = new Sessao(pauta, abertura, 5);
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessao));

        ResultadoResponse resultado = sut.apurar(1L);

        assertThat(resultado.votosSim()).isZero();
        assertThat(resultado.votosNao()).isZero();
        assertThat(resultado.totalVotos()).isZero();
        assertThat(resultado.statusSessao()).isEqualTo(StatusSessao.ABERTA);
        assertThat(resultado.timestamp()).isEqualTo(OffsetDateTime.now(clock));
        verify(votoRepository, never()).contarPorTipo(1L);
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
        assertThat(resultado.timestamp()).isEqualTo(OffsetDateTime.now(clock));
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
        assertThat(resultado.timestamp()).isEqualTo(OffsetDateTime.now(clock));
    }

    @Test
    @DisplayName("apurar pauta sem sessão deve retornar status NAO_INICIADA e contagens zeradas, sem consultar votos")
    void apurar_pautaSemSessao_retornaNaoIniciadaEZerado() {
        Pauta pauta = new Pauta("Título", "Descrição");
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.empty());

        ResultadoResponse resultado = sut.apurar(1L);

        assertThat(resultado.statusSessao()).isEqualTo(StatusSessao.NAO_INICIADA);
        assertThat(resultado.votosSim()).isZero();
        assertThat(resultado.votosNao()).isZero();
        assertThat(resultado.totalVotos()).isZero();
        assertThat(resultado.timestamp()).isEqualTo(OffsetDateTime.now(clock));
        verify(votoRepository, never()).contarPorTipo(1L);
    }

    @Test
    @DisplayName("apurar deve ler o Clock uma única vez, evitando status e timestamp de instantes diferentes")
    void apurar_leClockUmaUnicaVez_statusETimestampDoMesmoInstante() {
        Instant antesDoFechamento = FIXED_INSTANT;
        Instant depoisDoFechamento = FIXED_INSTANT.plusSeconds(600);
        ContadorDeLeiturasClock clockContador =
                new ContadorDeLeiturasClock(ZONE, List.of(antesDoFechamento, depoisDoFechamento));
        ResultadoService sutComClockContador =
                new ResultadoService(pautaService, sessaoRepository, votoRepository, clockContador);

        Pauta pauta = new Pauta("Título", "Descrição");
        LocalDateTime abertura = LocalDateTime.ofInstant(antesDoFechamento, ZONE).minusMinutes(1);
        Sessao sessao = new Sessao(pauta, abertura, 5);
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessao));

        ResultadoResponse resultado = sutComClockContador.apurar(1L);

        assertThat(clockContador.quantidadeDeLeituras()).isEqualTo(1);
        assertThat(resultado.statusSessao()).isEqualTo(StatusSessao.ABERTA);
        assertThat(resultado.timestamp())
                .isEqualTo(OffsetDateTime.ofInstant(antesDoFechamento, ZONE));
    }

    private static final class ContadorDeLeiturasClock extends Clock {
        private final ZoneId zona;
        private final List<Instant> instantes;
        private int indice = 0;

        private ContadorDeLeiturasClock(ZoneId zona, List<Instant> instantes) {
            this.zona = zona;
            this.instantes = instantes;
        }

        @Override
        public ZoneId getZone() {
            return zona;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            Instant instante = instantes.get(indice);
            indice++;
            return instante;
        }

        int quantidadeDeLeituras() {
            return indice;
        }
    }

    @Test
    @DisplayName("apurar deve ser somente leitura (readOnly), consistente com SessaoService/VotoService")
    void apurar_estaAnotadoComTransactionalReadOnly() throws NoSuchMethodException {
        Method apurar = ResultadoService.class.getMethod("apurar", Long.class);
        Transactional transactional = apurar.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
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

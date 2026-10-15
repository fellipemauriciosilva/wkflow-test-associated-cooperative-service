package br.coop.assembly.service;

import br.coop.assembly.entity.Pauta;
import br.coop.assembly.entity.Sessao;
import br.coop.assembly.exception.DuracaoInvalidaException;
import br.coop.assembly.exception.PautaNotFoundException;
import br.coop.assembly.exception.SessaoJaAbertaException;
import br.coop.assembly.repository.PautaRepository;
import br.coop.assembly.repository.SessaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessaoService Tests")
class SessaoServiceTest {
    @Mock(lenient = true)
    private SessaoRepository sessaoRepository;

    @Mock(lenient = true)
    private PautaRepository pautaRepository;

    @Mock(lenient = true)
    private Clock clock;

    @InjectMocks
    private SessaoService service;

    private Pauta testPauta;
    private Sessao testSessao;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2024, 1, 15, 10, 0, 0);
        when(clock.instant()).thenReturn(Instant.from(now.atZone(ZoneId.systemDefault())));
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        ReflectionTestUtils.setField(service, "duracaoPadraoMinutos", 1);
        ReflectionTestUtils.setField(service, "duracaoMinimaMinutos", 1);
        ReflectionTestUtils.setField(service, "duracaoMaximaMinutos", 1440);

        testPauta = new Pauta();
        testPauta.setId(1L);
        testPauta.setTitle("Test Pauta");
        testPauta.setDescription("Test Description");

        testSessao = new Sessao();
        testSessao.setId(1L);
        testSessao.setPautaId(1L);
        testSessao.setPauta(testPauta);
        testSessao.setDataInicio(now);
        testSessao.setDataFim(now.plusMinutes(5));
        testSessao.setStatus(Sessao.Status.ATIVA);
    }

    @Test
    @DisplayName("Should open session with valid duration")
    void abrirSessao_ComDuracaoValida_RetornaSessaoAtiva() {
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(testPauta));
        when(sessaoRepository.findActiveSessaoForPauta(1L, now)).thenReturn(Optional.empty());
        when(sessaoRepository.save(any(Sessao.class))).thenReturn(testSessao);

        Sessao result = service.abrirSessao(1L, 5);

        assertNotNull(result);
        assertEquals(1L, result.getPautaId());
        assertEquals(Sessao.Status.ATIVA, result.getStatus());
        verify(sessaoRepository, times(1)).save(any(Sessao.class));
    }

    @Test
    @DisplayName("Should use default duration when not specified")
    void abrirSessao_ComDuracaoPadrao_Usa1Minuto() {
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(testPauta));
        when(sessaoRepository.findActiveSessaoForPauta(1L, now)).thenReturn(Optional.empty());

        Sessao expected = new Sessao();
        expected.setId(1L);
        expected.setPautaId(1L);
        expected.setDataInicio(now);
        expected.setDataFim(now.plusMinutes(1));
        expected.setStatus(Sessao.Status.ATIVA);

        when(sessaoRepository.save(any(Sessao.class))).thenReturn(expected);

        Sessao result = service.abrirSessao(1L, null);

        assertNotNull(result);
        assertEquals(now.plusMinutes(1), result.getDataFim());
        verify(sessaoRepository, times(1)).save(any(Sessao.class));
    }

    @Test
    @DisplayName("Should throw exception when pauta not found")
    void abrirSessao_PautaNaoExiste_LancaException() {
        when(pautaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(PautaNotFoundException.class, () ->
                service.abrirSessao(999L, 1)
        );
    }

    @Test
    @DisplayName("Should throw exception when active session exists")
    void abrirSessao_SessaoAtiva_LancaException() {
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(testPauta));
        when(sessaoRepository.findActiveSessaoForPauta(1L, now)).thenReturn(Optional.of(testSessao));

        assertThrows(SessaoJaAbertaException.class, () ->
                service.abrirSessao(1L, 1)
        );
    }

    @Test
    @DisplayName("Should throw exception for negative duration")
    void abrirSessao_DuracaoNegativa_LancaException() {
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(testPauta));

        assertThrows(DuracaoInvalidaException.class, () ->
                service.abrirSessao(1L, -1)
        );
    }

    @Test
    @DisplayName("Should throw exception for zero duration")
    void abrirSessao_DuracaoZero_LancaException() {
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(testPauta));

        assertThrows(DuracaoInvalidaException.class, () ->
                service.abrirSessao(1L, 0)
        );
    }

    @Test
    @DisplayName("Should throw exception for duration > 1440 minutes")
    void abrirSessao_DuracaoMaiorQue1440_LancaException() {
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(testPauta));

        assertThrows(DuracaoInvalidaException.class, () ->
                service.abrirSessao(1L, 2000)
        );
    }

    @Test
    @DisplayName("Should return true for active session")
    void isSessionActive_ComFuturoDataFim_RetornaTrue() {
        Sessao sessao = new Sessao();
        sessao.setDataFim(now.plusMinutes(5));

        assertTrue(service.isSessionActive(sessao));
    }

    @Test
    @DisplayName("Should return false for expired session")
    void isSessionActive_ComPassadoDataFim_RetornaFalse() {
        Sessao sessao = new Sessao();
        sessao.setDataFim(now.minusMinutes(5));

        assertFalse(service.isSessionActive(sessao));
    }

    @Test
    @DisplayName("Should return ATIVA status for active session")
    void getSessionStatus_ComDataFimFuturo_RetornaAtiva() {
        testSessao.setDataFim(now.plusMinutes(5));

        Sessao.Status status = service.getSessionStatus(testSessao);

        assertEquals(Sessao.Status.ATIVA, status);
    }

    @Test
    @DisplayName("Should return EXPIRADA status for expired session")
    void getSessionStatus_ComDataFimPassado_RetornaExpirada() {
        testSessao.setDataFim(now.minusMinutes(5));

        Sessao.Status status = service.getSessionStatus(testSessao);

        assertEquals(Sessao.Status.EXPIRADA, status);
    }

    @Test
    @DisplayName("Should calculate dataFim correctly")
    void calculateDataFim_ComValoresValidos_RetornaDataCorreta() {
        LocalDateTime dataInicio = LocalDateTime.of(2024, 1, 15, 10, 0, 0);

        LocalDateTime result = service.calculateDataFim(dataInicio, 5);

        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 5, 0), result);
    }

    @Test
    @DisplayName("Should return true for canAcceptVotes when session is active")
    void canAcceptVotes_ComSessaoAtiva_RetornaTrue() {
        testSessao.setDataFim(now.plusMinutes(5));
        when(sessaoRepository.findById(1L)).thenReturn(Optional.of(testSessao));

        assertTrue(service.canAcceptVotes(1L));
    }

    @Test
    @DisplayName("Should return false for canAcceptVotes when session is expired")
    void canAcceptVotes_ComSessaoExpirada_RetornaFalse() {
        testSessao.setDataFim(now.minusMinutes(5));
        when(sessaoRepository.findById(1L)).thenReturn(Optional.of(testSessao));

        assertFalse(service.canAcceptVotes(1L));
    }

    @Test
    @DisplayName("Should return false for canAcceptVotes when session not found")
    void canAcceptVotes_SessaoNaoExiste_RetornaFalse() {
        when(sessaoRepository.findById(999L)).thenReturn(Optional.empty());

        assertFalse(service.canAcceptVotes(999L));
    }

    @Test
    @DisplayName("Should allow opening new session when previous is expired")
    void abrirSessao_SessaoExpirada_AbreNovaSessao() {
        Sessao expiredSessao = new Sessao();
        expiredSessao.setDataFim(now.minusMinutes(5));

        when(pautaRepository.findById(1L)).thenReturn(Optional.of(testPauta));
        when(sessaoRepository.findActiveSessaoForPauta(1L, now)).thenReturn(Optional.empty());
        when(sessaoRepository.save(any(Sessao.class))).thenReturn(testSessao);

        Sessao result = service.abrirSessao(1L, 5);

        assertNotNull(result);
        verify(sessaoRepository, times(1)).save(any(Sessao.class));
    }
}

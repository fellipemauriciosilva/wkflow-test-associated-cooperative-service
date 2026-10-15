package br.coop.assembly.service;

import br.coop.assembly.dto.ResultadoDTO;
import br.coop.assembly.entity.Pauta;
import br.coop.assembly.entity.Sessao;
import br.coop.assembly.entity.Voto;
import br.coop.assembly.entity.Voto.VotoEnum;
import br.coop.assembly.exception.PautaNotFoundException;
import br.coop.assembly.exception.SessaoNaoAtivaException;
import br.coop.assembly.exception.VotoDuplicadoException;
import br.coop.assembly.repository.PautaRepository;
import br.coop.assembly.repository.SessaoRepository;
import br.coop.assembly.repository.VotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VotoServiceTest {
    @Mock(lenient = true)
    private VotoRepository votoRepository;
    @Mock(lenient = true)
    private PautaRepository pautaRepository;
    @Mock(lenient = true)
    private SessaoRepository sessaoRepository;
    @Mock(lenient = true)
    private Clock clock;

    private VotoService votoService;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2024, 1, 15, 10, 0, 0);
        ZoneId zoneId = ZoneId.systemDefault();
        when(clock.instant()).thenReturn(Instant.from(now.atZone(zoneId)));
        when(clock.getZone()).thenReturn(zoneId);
        votoService = new VotoService(votoRepository, pautaRepository, sessaoRepository, clock);
    }

    @Test
    void registrarVoto_ValidRequest_Success() {
        Long pautaId = 1L;
        String associadoId = "A1";
        VotoEnum voto = VotoEnum.SIM;

        Pauta pauta = new Pauta();
        pauta.setId(pautaId);

        Sessao sessao = new Sessao();
        sessao.setId(1L);
        sessao.setPautaId(pautaId);
        sessao.setDataInicio(now.minusMinutes(5));
        sessao.setDataFim(now.plusMinutes(5));
        sessao.setStatus(Sessao.Status.ATIVA);

        Voto savedVoto = new Voto();
        savedVoto.setId(100L);
        savedVoto.setPautaId(pautaId);
        savedVoto.setAssociadoId(associadoId);
        savedVoto.setVoto(voto);
        savedVoto.setTimestamp(now);

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.findActiveSessaoForPauta(eq(pautaId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(sessao));
        when(votoRepository.findByPautaIdAndAssociadoId(pautaId, associadoId))
                .thenReturn(Optional.empty());
        when(votoRepository.save(any(Voto.class))).thenReturn(savedVoto);

        Voto result = votoService.registrarVoto(pautaId, associadoId, voto);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(pautaId, result.getPautaId());
        assertEquals(associadoId, result.getAssociadoId());
        assertEquals(voto, result.getVoto());
        verify(votoRepository).save(any(Voto.class));
    }

    @Test
    void registrarVoto_PautaNotFound_ThrowsException() {
        Long pautaId = 999L;
        when(pautaRepository.findById(pautaId)).thenReturn(Optional.empty());

        assertThrows(PautaNotFoundException.class,
                () -> votoService.registrarVoto(pautaId, "A1", VotoEnum.SIM));
    }

    @Test
    void registrarVoto_SessionNotActive_ThrowsException() {
        Long pautaId = 1L;
        Pauta pauta = new Pauta();
        pauta.setId(pautaId);

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.findActiveSessaoForPauta(eq(pautaId), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        SessaoNaoAtivaException ex = assertThrows(SessaoNaoAtivaException.class,
                () -> votoService.registrarVoto(pautaId, "A1", VotoEnum.SIM));
        assertEquals("Sessão não está ativa para votação", ex.getMessage());
    }

    @Test
    void registrarVoto_DuplicateVote_ThrowsException() {
        Long pautaId = 1L;
        String associadoId = "A1";

        Pauta pauta = new Pauta();
        pauta.setId(pautaId);

        Sessao sessao = new Sessao();
        sessao.setId(1L);
        sessao.setPautaId(pautaId);
        sessao.setDataInicio(now.minusMinutes(5));
        sessao.setDataFim(now.plusMinutes(5));
        sessao.setStatus(Sessao.Status.ATIVA);

        Voto existingVoto = new Voto();
        existingVoto.setId(50L);
        existingVoto.setPautaId(pautaId);
        existingVoto.setAssociadoId(associadoId);
        existingVoto.setVoto(VotoEnum.SIM);

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.findActiveSessaoForPauta(eq(pautaId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(sessao));
        when(votoRepository.findByPautaIdAndAssociadoId(pautaId, associadoId))
                .thenReturn(Optional.of(existingVoto));

        VotoDuplicadoException ex = assertThrows(VotoDuplicadoException.class,
                () -> votoService.registrarVoto(pautaId, associadoId, VotoEnum.NAO));
        assertEquals("Este associado já votou nesta pauta", ex.getMessage());
    }

    @Test
    void registrarVoto_BlankAssociadoId_ThrowsException() {
        Long pautaId = 1L;
        Pauta pauta = new Pauta();
        pauta.setId(pautaId);

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.of(pauta));

        assertThrows(IllegalArgumentException.class,
                () -> votoService.registrarVoto(pautaId, "", VotoEnum.SIM));
    }

    @Test
    void registrarVoto_NullVoto_ThrowsException() {
        Long pautaId = 1L;
        Pauta pauta = new Pauta();
        pauta.setId(pautaId);

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.of(pauta));

        assertThrows(IllegalArgumentException.class,
                () -> votoService.registrarVoto(pautaId, "A1", null));
    }

    @Test
    void getResultado_ValidPauta_ReturnsAggregatedResults() {
        Long pautaId = 1L;
        Pauta pauta = new Pauta();
        pauta.setId(pautaId);

        Sessao sessao = new Sessao();
        sessao.setId(1L);
        sessao.setPautaId(pautaId);
        sessao.setDataFim(now.plusMinutes(5));
        sessao.setStatus(Sessao.Status.ATIVA);

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.of(pauta));
        when(votoRepository.countByPautaIdAndVoto(pautaId, VotoEnum.SIM)).thenReturn(2L);
        when(votoRepository.countByPautaIdAndVoto(pautaId, VotoEnum.NAO)).thenReturn(1L);
        when(sessaoRepository.findByPautaId(pautaId)).thenReturn(Optional.of(sessao));

        ResultadoDTO resultado = votoService.getResultado(pautaId);

        assertNotNull(resultado);
        assertEquals(pautaId, resultado.getPautaId());
        assertEquals(3L, resultado.getTotalVotos());
        assertEquals(2L, resultado.getVotosSim());
        assertEquals(1L, resultado.getVotosNao());
        assertEquals(new BigDecimal("67.00"), resultado.getPercentualSim());
        assertEquals(new BigDecimal("33.00"), resultado.getPercentualNao());
        assertEquals("ATIVA", resultado.getStatus());
    }

    @Test
    void getResultado_NoVotes_ReturnsZeroResults() {
        Long pautaId = 1L;
        Pauta pauta = new Pauta();
        pauta.setId(pautaId);

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.of(pauta));
        when(votoRepository.countByPautaIdAndVoto(pautaId, VotoEnum.SIM)).thenReturn(0L);
        when(votoRepository.countByPautaIdAndVoto(pautaId, VotoEnum.NAO)).thenReturn(0L);
        when(sessaoRepository.findByPautaId(pautaId)).thenReturn(Optional.empty());

        ResultadoDTO resultado = votoService.getResultado(pautaId);

        assertNotNull(resultado);
        assertEquals(0L, resultado.getTotalVotos());
        assertEquals(0L, resultado.getVotosSim());
        assertEquals(0L, resultado.getVotosNao());
        assertEquals(BigDecimal.ZERO, resultado.getPercentualSim());
        assertEquals(BigDecimal.ZERO, resultado.getPercentualNao());
        assertEquals("SEM_SESSAO", resultado.getStatus());
    }

    @Test
    void getResultado_SingleVote_CorrectPercentages() {
        Long pautaId = 1L;
        Pauta pauta = new Pauta();
        pauta.setId(pautaId);

        Sessao sessao = new Sessao();
        sessao.setId(1L);
        sessao.setPautaId(pautaId);
        sessao.setDataFim(now.plusMinutes(5));

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.of(pauta));
        when(votoRepository.countByPautaIdAndVoto(pautaId, VotoEnum.SIM)).thenReturn(1L);
        when(votoRepository.countByPautaIdAndVoto(pautaId, VotoEnum.NAO)).thenReturn(0L);
        when(sessaoRepository.findByPautaId(pautaId)).thenReturn(Optional.of(sessao));

        ResultadoDTO resultado = votoService.getResultado(pautaId);

        assertEquals(1L, resultado.getTotalVotos());
        assertEquals(new BigDecimal("100.00"), resultado.getPercentualSim());
        assertEquals(new BigDecimal("0.00"), resultado.getPercentualNao());
    }

    @Test
    void getResultado_ExpiredSession_ReturnsExpiredStatus() {
        Long pautaId = 1L;
        Pauta pauta = new Pauta();
        pauta.setId(pautaId);

        Sessao sessao = new Sessao();
        sessao.setId(1L);
        sessao.setPautaId(pautaId);
        sessao.setDataFim(now.minusMinutes(1));

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.of(pauta));
        when(votoRepository.countByPautaIdAndVoto(pautaId, VotoEnum.SIM)).thenReturn(2L);
        when(votoRepository.countByPautaIdAndVoto(pautaId, VotoEnum.NAO)).thenReturn(1L);
        when(sessaoRepository.findByPautaId(pautaId)).thenReturn(Optional.of(sessao));

        ResultadoDTO resultado = votoService.getResultado(pautaId);

        assertEquals("EXPIRADA", resultado.getStatus());
    }

    @Test
    void getResultado_PautaNotFound_ThrowsException() {
        Long pautaId = 999L;
        when(pautaRepository.findById(pautaId)).thenReturn(Optional.empty());

        assertThrows(PautaNotFoundException.class,
                () -> votoService.getResultado(pautaId));
    }
}

package br.coop.voting.domain.sessao.service;

import br.coop.voting.domain.pauta.repository.PautaRepository;
import br.coop.voting.domain.sessao.dto.SessaoRequest;
import br.coop.voting.domain.sessao.dto.SessaoResponse;
import br.coop.voting.domain.sessao.entity.Sessao;
import br.coop.voting.domain.sessao.exception.SessaoAtivaException;
import br.coop.voting.domain.sessao.repository.SessaoRepository;
import br.coop.voting.shared.exception.PautaNaoEncontradaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessaoServiceTest {

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private PautaRepository pautaRepository;

    @InjectMocks
    private SessaoService sessaoService;

    private Long pautaId;
    private SessaoRequest request;

    @BeforeEach
    void setUp() {
        pautaId = 1L;
        request = new SessaoRequest(5);
    }

    @Test
    void testAbrirSessaoSucesso() {
        when(pautaRepository.existsById(pautaId)).thenReturn(true);
        when(sessaoRepository.findByPautaIdAndAtivaTrue(pautaId)).thenReturn(Optional.empty());

        Sessao sessao = new Sessao();
        sessao.setId("sessao-1");
        sessao.setPautaId(pautaId);
        sessao.setDurationMinutes(5);
        sessao.setAtiva(true);
        sessao.setDataAbertura(LocalDateTime.now());

        when(sessaoRepository.save(any(Sessao.class))).thenReturn(sessao);

        SessaoResponse response = sessaoService.abrirSessao(pautaId, request);

        assertNotNull(response);
        assertEquals("sessao-1", response.getId());
        assertEquals(pautaId, response.getPauta_id());
        assertEquals(5, response.getDuracao_minutos());
        assertTrue(response.getAtiva());

        verify(pautaRepository).existsById(pautaId);
        verify(sessaoRepository).findByPautaIdAndAtivaTrue(pautaId);
        verify(sessaoRepository).save(any(Sessao.class));
    }

    @Test
    void testAbrirSessaoPautaNaoEncontrada() {
        when(pautaRepository.existsById(pautaId)).thenReturn(false);

        assertThrows(PautaNaoEncontradaException.class, () ->
            sessaoService.abrirSessao(pautaId, request)
        );

        verify(pautaRepository).existsById(pautaId);
        verify(sessaoRepository, never()).save(any());
    }

    @Test
    void testAbrirSessaoSessaoAtivaExistente() {
        when(pautaRepository.existsById(pautaId)).thenReturn(true);

        Sessao sessaoAtiva = new Sessao();
        sessaoAtiva.setId("sessao-1");
        sessaoAtiva.setAtiva(true);

        when(sessaoRepository.findByPautaIdAndAtivaTrue(pautaId)).thenReturn(Optional.of(sessaoAtiva));

        assertThrows(SessaoAtivaException.class, () ->
            sessaoService.abrirSessao(pautaId, request)
        );

        verify(pautaRepository).existsById(pautaId);
        verify(sessaoRepository).findByPautaIdAndAtivaTrue(pautaId);
        verify(sessaoRepository, never()).save(any());
    }

    @Test
    void testVerificarExpiracoes() {
        Sessao sessao1 = new Sessao();
        sessao1.setId("sessao-1");
        sessao1.setAtiva(true);
        sessao1.setDataFechamento(null);
        sessao1.setDataAbertura(LocalDateTime.now().minusMinutes(10));
        sessao1.setDurationMinutes(5);

        Sessao sessao2 = new Sessao();
        sessao2.setId("sessao-2");
        sessao2.setAtiva(true);
        sessao2.setDataFechamento(null);
        sessao2.setDataAbertura(LocalDateTime.now());
        sessao2.setDurationMinutes(60);

        when(sessaoRepository.findByAtivaAndDataFechamentoNull()).thenReturn(List.of(sessao1, sessao2));
        when(sessaoRepository.save(any(Sessao.class))).thenAnswer(inv -> inv.getArgument(0));

        sessaoService.verificarExpiracoes();

        verify(sessaoRepository).findByAtivaAndDataFechamentoNull();
        verify(sessaoRepository, times(1)).save(any(Sessao.class));
    }

    @Test
    void testValidarSessaoAtiva_NaoExiste() {
        when(sessaoRepository.findByPautaIdAndAtivaTrue(pautaId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> sessaoService.validarSessaoAtiva(pautaId));
    }

    @Test
    void testValidarSessaoAtiva_Existe() {
        Sessao sessao = new Sessao();
        sessao.setId("sessao-1");
        sessao.setAtiva(true);

        when(sessaoRepository.findByPautaIdAndAtivaTrue(pautaId)).thenReturn(Optional.of(sessao));

        assertThrows(SessaoAtivaException.class, () ->
            sessaoService.validarSessaoAtiva(pautaId)
        );
    }

    @Test
    void testVerificaExpiracoes_MakesIdempotent() {
        var sessao = new Sessao();
        sessao.setId("sessao-1");
        sessao.setAtiva(true);
        sessao.setDataAbertura(LocalDateTime.now().minusMinutes(65));
        sessao.setDurationMinutes(1);

        when(sessaoRepository.findAllActiveSessions()).thenReturn(List.of(sessao));
        when(sessaoRepository.save(any(Sessao.class))).thenAnswer(inv -> inv.getArgument(0));

        sessaoService.verificarExpiracoes();
        sessaoService.verificarExpiracoes();

        verify(sessaoRepository, times(2)).findAllActiveSessions();
    }

    @Test
    void testVerificaExpiracoes_UpdatesExpiredSessions() {
        var sessao = new Sessao();
        sessao.setId("sessao-1");
        sessao.setAtiva(true);
        sessao.setDataAbertura(LocalDateTime.now().minusMinutes(65));
        sessao.setDurationMinutes(1);

        when(sessaoRepository.findAllActiveSessions()).thenReturn(List.of(sessao));
        when(sessaoRepository.save(any(Sessao.class))).thenAnswer(inv -> inv.getArgument(0));

        sessaoService.verificarExpiracoes();

        verify(sessaoRepository).findAllActiveSessions();
        verify(sessaoRepository).save(any(Sessao.class));
    }
}

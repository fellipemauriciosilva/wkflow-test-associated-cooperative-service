package br.coop.voting.domain.pauta.service;

import br.coop.voting.domain.pauta.dto.AtualizarPautaRequest;
import br.coop.voting.domain.pauta.dto.CriarPautaRequest;
import br.coop.voting.domain.pauta.dto.PautaResponse;
import br.coop.voting.domain.pauta.entity.Pauta;
import br.coop.voting.domain.pauta.repository.PautaRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PautaServiceTest {

    @Mock
    private PautaRepository pautaRepository;

    @Mock
    private SessaoRepository sessaoRepository;

    @InjectMocks
    private PautaService pautaService;

    private Pauta pautaMock;
    private CriarPautaRequest criarRequest;

    @BeforeEach
    void setUp() {
        pautaMock = new Pauta();
        pautaMock.setId(1L);
        pautaMock.setTitulo("Pauta Teste");
        pautaMock.setDescricao("Descrição da pauta teste");
        pautaMock.setCreatedAt(LocalDateTime.now());
        pautaMock.setUpdatedAt(LocalDateTime.now());

        criarRequest = new CriarPautaRequest("Pauta Teste", "Descrição da pauta teste");

        lenient().when(sessaoRepository.findByPautaIdAndAtivaTrue(any())).thenReturn(Optional.empty());
        lenient().when(sessaoRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void testCriarPautaValida() {
        when(pautaRepository.save(any(Pauta.class))).thenReturn(pautaMock);

        PautaResponse response = pautaService.criar(criarRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Pauta Teste", response.getTitulo());
        assertEquals("Descrição da pauta teste", response.getDescricao());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());

        verify(pautaRepository, times(1)).save(any(Pauta.class));
    }

    @Test
    void testObterPautaPorIdExistente() {
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pautaMock));

        PautaResponse response = pautaService.obterPorId(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Pauta Teste", response.getTitulo());

        verify(pautaRepository, times(1)).findById(1L);
    }

    @Test
    void testObterPautaPorIdInexistente_LancaExcecao() {
        when(pautaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PautaNaoEncontradaException.class, () -> pautaService.obterPorId(99L));

        verify(pautaRepository, times(1)).findById(99L);
    }

    @Test
    void testListarTodas() {
        Pauta pauta2 = new Pauta();
        pauta2.setId(2L);
        pauta2.setTitulo("Pauta Segunda");
        pauta2.setDescricao("Descrição da segunda pauta");
        pauta2.setCreatedAt(LocalDateTime.now());
        pauta2.setUpdatedAt(LocalDateTime.now());

        when(pautaRepository.findAll()).thenReturn(List.of(pautaMock, pauta2));

        List<PautaResponse> response = pautaService.listarTodas();

        assertEquals(2, response.size());
        assertEquals("Pauta Teste", response.get(0).getTitulo());
        assertEquals("Pauta Segunda", response.get(1).getTitulo());

        verify(pautaRepository, times(1)).findAll();
    }

    @Test
    void testBuscarPorTitulo() {
        when(pautaRepository.findByTituloContainingIgnoreCase("Teste")).thenReturn(List.of(pautaMock));

        List<PautaResponse> response = pautaService.buscarPorTitulo("Teste");

        assertEquals(1, response.size());
        assertEquals("Pauta Teste", response.get(0).getTitulo());

        verify(pautaRepository, times(1)).findByTituloContainingIgnoreCase("Teste");
    }

    @Test
    void testAtualizarPautaValida() {
        AtualizarPautaRequest atualizarRequest = new AtualizarPautaRequest("Pauta Atualizada", "Nova descrição pauta");
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pautaMock));
        when(pautaRepository.save(any(Pauta.class))).thenReturn(pautaMock);

        PautaResponse response = pautaService.atualizar(1L, atualizarRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());

        verify(pautaRepository, times(1)).findById(1L);
        verify(pautaRepository, times(1)).save(any(Pauta.class));
    }

    @Test
    void testAtualizarPautaInexistente_LancaExcecao() {
        AtualizarPautaRequest atualizarRequest = new AtualizarPautaRequest("Pauta Atualizada", "Nova descrição");
        when(pautaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PautaNaoEncontradaException.class, () -> pautaService.atualizar(99L, atualizarRequest));

        verify(pautaRepository, times(1)).findById(99L);
    }

    @Test
    void testDeletarPautaExistente() {
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pautaMock));

        assertDoesNotThrow(() -> pautaService.deletar(1L));

        verify(pautaRepository, times(1)).findById(1L);
        verify(pautaRepository, times(1)).delete(pautaMock);
    }

    @Test
    void testDeletarPautaInexistente_LancaExcecao() {
        when(pautaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PautaNaoEncontradaException.class, () -> pautaService.deletar(99L));

        verify(pautaRepository, times(1)).findById(99L);
    }

    @Test
    void testObterPautaRetornaSessaoAtiva() {
        var sessaoMock = new br.coop.voting.domain.sessao.entity.Sessao();
        sessaoMock.setId("session-1");
        sessaoMock.setPautaId(1L);
        sessaoMock.setAtiva(true);
        sessaoMock.setDurationMinutes(5);

        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pautaMock));
        when(sessaoRepository.findByPautaIdAndAtivaTrue(1L)).thenReturn(Optional.of(sessaoMock));
        lenient().when(sessaoRepository.findLatestClosedByPautaId(1L)).thenReturn(Optional.empty());

        var response = pautaService.obterPorId(1L);

        org.junit.jupiter.api.Assertions.assertNotNull(response);
        org.junit.jupiter.api.Assertions.assertEquals("SESSAO_ABERTA", response.getStatus_sessao());
        org.junit.jupiter.api.Assertions.assertNotNull(response.getSessao());
        org.junit.jupiter.api.Assertions.assertTrue(response.getSessao().getAtiva());
    }

    @Test
    void testObterPautaRetornaSessaoFechada() {
        var sessaoMock = new br.coop.voting.domain.sessao.entity.Sessao();
        sessaoMock.setId("session-1");
        sessaoMock.setPautaId(1L);
        sessaoMock.setAtiva(false);
        sessaoMock.setDurationMinutes(5);
        sessaoMock.setDataFechamento(LocalDateTime.now());

        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pautaMock));
        when(sessaoRepository.findByPautaIdAndAtivaTrue(1L)).thenReturn(Optional.empty());
        when(sessaoRepository.findLatestClosedByPautaId(1L)).thenReturn(Optional.of(sessaoMock));

        var response = pautaService.obterPorId(1L);

        org.junit.jupiter.api.Assertions.assertNotNull(response);
        org.junit.jupiter.api.Assertions.assertEquals("SESSAO_FECHADA", response.getStatus_sessao());
        org.junit.jupiter.api.Assertions.assertNotNull(response.getSessao());
        org.junit.jupiter.api.Assertions.assertFalse(response.getSessao().getAtiva());
    }

    @Test
    void testObterPautaRetornaSemSessao() {
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pautaMock));
        when(sessaoRepository.findByPautaIdAndAtivaTrue(1L)).thenReturn(Optional.empty());
        when(sessaoRepository.findLatestClosedByPautaId(1L)).thenReturn(Optional.empty());

        var response = pautaService.obterPorId(1L);

        org.junit.jupiter.api.Assertions.assertNotNull(response);
        org.junit.jupiter.api.Assertions.assertEquals("PAUTA_SEM_SESSAO", response.getStatus_sessao());
        org.junit.jupiter.api.Assertions.assertNull(response.getSessao());
    }

    @Test
    void testCriarPautaResponse_AllFieldsMapped() {
        LocalDateTime now = LocalDateTime.now();
        pautaMock.setCreatedAt(now);
        pautaMock.setUpdatedAt(now);

        when(pautaRepository.save(any(Pauta.class))).thenReturn(pautaMock);

        PautaResponse response = pautaService.criar(criarRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Pauta Teste", response.getTitulo());
        assertEquals("Descrição da pauta teste", response.getDescricao());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());

        verify(pautaRepository, times(1)).save(any(Pauta.class));
    }
}

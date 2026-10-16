package br.coop.voting.domain.voto.service;

import br.coop.voting.domain.pauta.entity.Pauta;
import br.coop.voting.domain.pauta.repository.PautaRepository;
import br.coop.voting.domain.sessao.entity.Sessao;
import br.coop.voting.domain.sessao.repository.SessaoRepository;
import br.coop.voting.domain.voto.dto.VotoRequestDTO;
import br.coop.voting.domain.voto.dto.VotoResponseDTO;
import br.coop.voting.domain.voto.dto.VotoResultadoDTO;
import br.coop.voting.domain.voto.entity.Voto;
import br.coop.voting.domain.voto.entity.VotoEnum;
import br.coop.voting.domain.voto.exception.VotacaoFechadaException;
import br.coop.voting.domain.voto.exception.VotoDuplicadoException;
import br.coop.voting.domain.voto.repository.VotoRepository;
import br.coop.voting.shared.exception.PautaNaoEncontradaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VotoServiceTest {

    @Mock
    private VotoRepository votoRepository;

    @Mock
    private PautaRepository pautaRepository;

    @Mock
    private SessaoRepository sessaoRepository;

    @InjectMocks
    private VotoService votoService;

    @Test
    void testRegistrarVoto_Sucesso() {
        Long pautaId = 1L;
        VotoRequestDTO request = new VotoRequestDTO("A123", "SIM");

        Pauta pauta = new Pauta();
        pauta.setId(pautaId);

        Sessao sessao = new Sessao();
        sessao.setId("sess-1");
        sessao.setPautaId(pautaId);
        sessao.setAtiva(true);

        Voto voto = new Voto();
        voto.setId(1L);
        voto.setPautaId(pautaId);
        voto.setSessaoId("sess-1");
        voto.setIdAssociado("A123");
        voto.setVoto(VotoEnum.SIM);
        voto.setCreatedAt(LocalDateTime.now());

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.findByPautaIdAndAtivaTrue(pautaId)).thenReturn(Optional.of(sessao));
        when(votoRepository.findByPautaIdAndIdAssociado(pautaId, "A123")).thenReturn(Optional.empty());
        when(votoRepository.save(any(Voto.class))).thenReturn(voto);

        VotoResponseDTO response = votoService.registrarVoto(pautaId, request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(pautaId, response.pautaId());
        assertEquals("A123", response.idAssociado());
        assertEquals("SIM", response.voto());

        verify(votoRepository, times(1)).save(any(Voto.class));
    }

    @Test
    void testRegistrarVoto_PautaNaoEncontrada() {
        Long pautaId = 999L;
        VotoRequestDTO request = new VotoRequestDTO("A123", "SIM");

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.empty());

        assertThrows(PautaNaoEncontradaException.class, () -> votoService.registrarVoto(pautaId, request));

        verify(votoRepository, never()).save(any());
    }

    @Test
    void testRegistrarVoto_VotacaoFechada() {
        Long pautaId = 1L;
        VotoRequestDTO request = new VotoRequestDTO("A123", "SIM");

        Pauta pauta = new Pauta();
        pauta.setId(pautaId);

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.findByPautaIdAndAtivaTrue(pautaId)).thenReturn(Optional.empty());

        assertThrows(VotacaoFechadaException.class, () -> {
            votoService.registrarVoto(pautaId, request);
        });

        verify(votoRepository, never()).save(any());
    }

    @Test
    void testRegistrarVoto_VotoDuplicado() {
        Long pautaId = 1L;
        VotoRequestDTO request = new VotoRequestDTO("A123", "SIM");

        Pauta pauta = new Pauta();
        pauta.setId(pautaId);

        Sessao sessao = new Sessao();
        sessao.setId("sess-1");
        sessao.setAtiva(true);

        Voto votoExistente = new Voto();
        votoExistente.setId(1L);

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.findByPautaIdAndAtivaTrue(pautaId)).thenReturn(Optional.of(sessao));
        when(votoRepository.findByPautaIdAndIdAssociado(pautaId, "A123")).thenReturn(Optional.of(votoExistente));

        assertThrows(VotoDuplicadoException.class, () -> {
            votoService.registrarVoto(pautaId, request);
        });

        verify(votoRepository, never()).save(any());
    }

    @Test
    void testRegistrarVoto_VotoNAO() {
        Long pautaId = 1L;
        VotoRequestDTO request = new VotoRequestDTO("A123", "NAO");

        Pauta pauta = new Pauta();
        pauta.setId(pautaId);

        Sessao sessao = new Sessao();
        sessao.setId("sess-1");
        sessao.setAtiva(true);

        Voto voto = new Voto();
        voto.setId(1L);
        voto.setPautaId(pautaId);
        voto.setSessaoId("sess-1");
        voto.setIdAssociado("A123");
        voto.setVoto(VotoEnum.NAO);
        voto.setCreatedAt(LocalDateTime.now());

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.findByPautaIdAndAtivaTrue(pautaId)).thenReturn(Optional.of(sessao));
        when(votoRepository.findByPautaIdAndIdAssociado(pautaId, "A123")).thenReturn(Optional.empty());
        when(votoRepository.save(any(Voto.class))).thenReturn(voto);

        VotoResponseDTO response = votoService.registrarVoto(pautaId, request);

        assertNotNull(response);
        assertEquals("NAO", response.voto());
    }

    @Test
    void testRegistrarVoto_CaseInsensitive() {
        Long pautaId = 1L;
        VotoRequestDTO request = new VotoRequestDTO("A123", "sim");

        Pauta pauta = new Pauta();
        pauta.setId(pautaId);

        Sessao sessao = new Sessao();
        sessao.setId("sess-1");
        sessao.setAtiva(true);

        Voto voto = new Voto();
        voto.setId(1L);
        voto.setPautaId(pautaId);
        voto.setSessaoId("sess-1");
        voto.setIdAssociado("A123");
        voto.setVoto(VotoEnum.SIM);
        voto.setCreatedAt(LocalDateTime.now());

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.findByPautaIdAndAtivaTrue(pautaId)).thenReturn(Optional.of(sessao));
        when(votoRepository.findByPautaIdAndIdAssociado(pautaId, "A123")).thenReturn(Optional.empty());
        when(votoRepository.save(any(Voto.class))).thenReturn(voto);

        VotoResponseDTO response = votoService.registrarVoto(pautaId, request);

        assertNotNull(response);
        assertEquals("SIM", response.voto());
    }

    @Test
    void testConsultarResultado_ComVotos() {
        Long pautaId = 1L;

        Pauta pauta = new Pauta();
        pauta.setId(pautaId);

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.of(pauta));
        when(votoRepository.countByPautaIdAndVoto(pautaId, VotoEnum.SIM)).thenReturn(3L);
        when(votoRepository.countByPautaIdAndVoto(pautaId, VotoEnum.NAO)).thenReturn(2L);

        VotoResultadoDTO resultado = votoService.consultarResultado(pautaId);

        assertNotNull(resultado);
        assertEquals(pautaId, resultado.pautaId());
        assertEquals(3L, resultado.sim());
        assertEquals(2L, resultado.nao());
    }

    @Test
    void testConsultarResultado_SemVotos() {
        Long pautaId = 1L;

        Pauta pauta = new Pauta();
        pauta.setId(pautaId);

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.of(pauta));
        when(votoRepository.countByPautaIdAndVoto(pautaId, VotoEnum.SIM)).thenReturn(0L);
        when(votoRepository.countByPautaIdAndVoto(pautaId, VotoEnum.NAO)).thenReturn(0L);

        VotoResultadoDTO resultado = votoService.consultarResultado(pautaId);

        assertNotNull(resultado);
        assertEquals(0L, resultado.sim());
        assertEquals(0L, resultado.nao());
    }

    @Test
    void testConsultarResultado_PautaNaoEncontrada() {
        Long pautaId = 999L;

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.empty());

        assertThrows(PautaNaoEncontradaException.class, () -> votoService.consultarResultado(pautaId));
    }
}

package br.coop.voting.domain.voto.controller;

import br.coop.voting.domain.voto.dto.VotoRequestDTO;
import br.coop.voting.domain.voto.dto.VotoResponseDTO;
import br.coop.voting.domain.voto.dto.VotoResultadoDTO;
import br.coop.voting.domain.voto.exception.VotacaoFechadaException;
import br.coop.voting.domain.voto.exception.VotoDuplicadoException;
import br.coop.voting.domain.voto.service.VotoService;
import br.coop.voting.shared.exception.PautaNaoEncontradaException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VotoController.class)
class VotoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VotoService votoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testRegistrarVoto_Sucesso() throws Exception {
        Long pautaId = 1L;
        VotoRequestDTO request = new VotoRequestDTO("A123", "SIM");
        VotoResponseDTO response = new VotoResponseDTO(1L, pautaId, "A123", "SIM", LocalDateTime.now());

        when(votoService.registrarVoto(eq(pautaId), any(VotoRequestDTO.class)))
            .thenReturn(response);

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.pautaId").value(pautaId))
            .andExpect(jsonPath("$.idAssociado").value("A123"))
            .andExpect(jsonPath("$.voto").value("SIM"));
    }

    @Test
    void testRegistrarVoto_VotoNAO() throws Exception {
        Long pautaId = 1L;
        VotoRequestDTO request = new VotoRequestDTO("A123", "NAO");
        VotoResponseDTO response = new VotoResponseDTO(1L, pautaId, "A123", "NAO", LocalDateTime.now());

        when(votoService.registrarVoto(eq(pautaId), any(VotoRequestDTO.class)))
            .thenReturn(response);

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.voto").value("NAO"));
    }

    @Test
    void testRegistrarVoto_VotacaoFechada() throws Exception {
        Long pautaId = 1L;
        VotoRequestDTO request = new VotoRequestDTO("A123", "SIM");

        when(votoService.registrarVoto(eq(pautaId), any(VotoRequestDTO.class)))
            .thenThrow(new VotacaoFechadaException(pautaId));

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VotacaoFechadaException"));
    }

    @Test
    void testRegistrarVoto_VotoDuplicado() throws Exception {
        Long pautaId = 1L;
        VotoRequestDTO request = new VotoRequestDTO("A123", "SIM");

        when(votoService.registrarVoto(eq(pautaId), any(VotoRequestDTO.class)))
            .thenThrow(new VotoDuplicadoException(pautaId, "A123"));

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("VotoDuplicadoException"));
    }

    @Test
    void testRegistrarVoto_PautaNaoEncontrada() throws Exception {
        Long pautaId = 999L;
        VotoRequestDTO request = new VotoRequestDTO("A123", "SIM");

        when(votoService.registrarVoto(eq(pautaId), any(VotoRequestDTO.class)))
            .thenThrow(new PautaNaoEncontradaException("Pauta " + pautaId + " não encontrada"));

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void testRegistrarVoto_VotoInvalido() throws Exception {
        Long pautaId = 1L;
        String invalidRequest = "{\"idAssociado\":\"A123\",\"voto\":\"TALVEZ\"}";

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testRegistrarVoto_IdAssociadoEmBranco() throws Exception {
        Long pautaId = 1L;
        String invalidRequest = "{\"idAssociado\":\"\",\"voto\":\"SIM\"}";

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testRegistrarVoto_VotoEmBranco() throws Exception {
        Long pautaId = 1L;
        String invalidRequest = "{\"idAssociado\":\"A123\",\"voto\":\"\"}";

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testConsultarResultado_ComVotos() throws Exception {
        Long pautaId = 1L;
        VotoResultadoDTO resultado = new VotoResultadoDTO(pautaId, 3L, 2L);

        when(votoService.consultarResultado(pautaId))
            .thenReturn(resultado);

        mockMvc.perform(get("/pautas/{id}/resultado", pautaId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pautaId").value(pautaId))
            .andExpect(jsonPath("$.sim").value(3L))
            .andExpect(jsonPath("$.nao").value(2L));
    }

    @Test
    void testConsultarResultado_SemVotos() throws Exception {
        Long pautaId = 1L;
        VotoResultadoDTO resultado = new VotoResultadoDTO(pautaId, 0L, 0L);

        when(votoService.consultarResultado(pautaId))
            .thenReturn(resultado);

        mockMvc.perform(get("/pautas/{id}/resultado", pautaId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sim").value(0L))
            .andExpect(jsonPath("$.nao").value(0L));
    }

    @Test
    void testConsultarResultado_PautaNaoEncontrada() throws Exception {
        Long pautaId = 999L;

        when(votoService.consultarResultado(pautaId))
            .thenThrow(new PautaNaoEncontradaException("Pauta " + pautaId + " não encontrada"));

        mockMvc.perform(get("/pautas/{id}/resultado", pautaId))
            .andExpect(status().isNotFound());
    }
}

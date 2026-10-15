package br.coop.assembly.controller;

import br.coop.assembly.dto.RegistrarVotoRequest;
import br.coop.assembly.dto.ResultadoDTO;
import br.coop.assembly.entity.Voto;
import br.coop.assembly.entity.Voto.VotoEnum;
import br.coop.assembly.exception.PautaNotFoundException;
import br.coop.assembly.exception.SessaoNaoAtivaException;
import br.coop.assembly.exception.VotoDuplicadoException;
import br.coop.assembly.service.VotoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VotoController.class)
class VotoControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VotoService votoService;

    @Test
    void registrarVoto_ValidRequest_Returns201() throws Exception {
        Long pautaId = 1L;
        RegistrarVotoRequest request = new RegistrarVotoRequest("A1", VotoEnum.SIM);

        Voto voto = new Voto();
        voto.setId(100L);
        voto.setPautaId(pautaId);
        voto.setAssociadoId("A1");
        voto.setVoto(VotoEnum.SIM);
        voto.setTimestamp(LocalDateTime.of(2024, 1, 15, 10, 0, 0));

        when(votoService.registrarVoto(eq(pautaId), eq("A1"), eq(VotoEnum.SIM)))
                .thenReturn(voto);

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.pautaId").value(pautaId))
                .andExpect(jsonPath("$.associadoId").value("A1"))
                .andExpect(jsonPath("$.voto").value("SIM"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void registrarVoto_SessionNotActive_Returns400() throws Exception {
        Long pautaId = 1L;
        RegistrarVotoRequest request = new RegistrarVotoRequest("A1", VotoEnum.SIM);

        when(votoService.registrarVoto(eq(pautaId), any(), any()))
                .thenThrow(new SessaoNaoAtivaException("Sessão não está ativa para votação"));

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_ACTIVE"))
                .andExpect(jsonPath("$.message").value("Sessão não está ativa para votação"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void registrarVoto_DuplicateVote_Returns409() throws Exception {
        Long pautaId = 1L;
        RegistrarVotoRequest request = new RegistrarVotoRequest("A1", VotoEnum.NAO);

        when(votoService.registrarVoto(eq(pautaId), any(), any()))
                .thenThrow(new VotoDuplicadoException("Este associado já votou nesta pauta"));

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VOTO_DUPLICADO"))
                .andExpect(jsonPath("$.message").value("Este associado já votou nesta pauta"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }


    @Test
    void registrarVoto_MissingAssociadoId_Returns400() throws Exception {
        Long pautaId = 1L;

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"voto\": \"SIM\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarVoto_BlankAssociadoId_Returns400() throws Exception {
        Long pautaId = 1L;
        RegistrarVotoRequest request = new RegistrarVotoRequest("  ", VotoEnum.SIM);

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarVoto_PautaNotFound_Returns404() throws Exception {
        Long pautaId = 999L;
        RegistrarVotoRequest request = new RegistrarVotoRequest("A1", VotoEnum.SIM);

        when(votoService.registrarVoto(eq(pautaId), any(), any()))
                .thenThrow(new PautaNotFoundException(pautaId));

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getResultado_ValidPauta_Returns200() throws Exception {
        Long pautaId = 1L;

        ResultadoDTO resultado = ResultadoDTO.builder()
                .pautaId(pautaId)
                .totalVotos(3L)
                .votosSim(2L)
                .votosNao(1L)
                .percentualSim(new BigDecimal("67.00"))
                .percentualNao(new BigDecimal("33.00"))
                .status("ATIVA")
                .build();

        when(votoService.getResultado(pautaId)).thenReturn(resultado);

        mockMvc.perform(get("/pautas/{id}/resultado", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pautaId").value(pautaId))
                .andExpect(jsonPath("$.totalVotos").value(3L))
                .andExpect(jsonPath("$.votosSim").value(2L))
                .andExpect(jsonPath("$.votosNao").value(1L))
                .andExpect(jsonPath("$.percentualSim").value(67.0))
                .andExpect(jsonPath("$.percentualNao").value(33.0))
                .andExpect(jsonPath("$.status").value("ATIVA"));
    }

    @Test
    void getResultado_NoVotes_Returns200WithZeros() throws Exception {
        Long pautaId = 1L;

        ResultadoDTO resultado = ResultadoDTO.builder()
                .pautaId(pautaId)
                .totalVotos(0L)
                .votosSim(0L)
                .votosNao(0L)
                .percentualSim(BigDecimal.ZERO)
                .percentualNao(BigDecimal.ZERO)
                .status("SEM_SESSAO")
                .build();

        when(votoService.getResultado(pautaId)).thenReturn(resultado);

        mockMvc.perform(get("/pautas/{id}/resultado", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotos").value(0L))
                .andExpect(jsonPath("$.percentualSim").value(0.0))
                .andExpect(jsonPath("$.status").value("SEM_SESSAO"));
    }

    @Test
    void getResultado_PautaNotFound_Returns404() throws Exception {
        Long pautaId = 999L;

        when(votoService.getResultado(pautaId))
                .thenThrow(new PautaNotFoundException(pautaId));

        mockMvc.perform(get("/pautas/{id}/resultado", pautaId))
                .andExpect(status().isNotFound());
    }
}

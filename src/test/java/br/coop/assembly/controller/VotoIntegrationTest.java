package br.coop.assembly.controller;

import br.coop.assembly.dto.AbrirSessaoRequest;
import br.coop.assembly.dto.CreatePautaRequest;
import br.coop.assembly.dto.PautaResponse;
import br.coop.assembly.dto.RegistrarVotoRequest;
import br.coop.assembly.entity.Voto.VotoEnum;
import br.coop.assembly.repository.PautaRepository;
import br.coop.assembly.repository.SessaoRepository;
import br.coop.assembly.repository.VotoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class VotoIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PautaRepository pautaRepository;

    @Autowired
    private SessaoRepository sessaoRepository;

    @Autowired
    private VotoRepository votoRepository;

    @BeforeEach
    void setUp() {
        votoRepository.deleteAll();
        sessaoRepository.deleteAll();
        pautaRepository.deleteAll();
    }

    @Test
    void integrationTest_CreatePauta_OpenSession_RegisterVotes_GetResults() throws Exception {
        // Create Pauta
        CreatePautaRequest pautaRequest = new CreatePautaRequest();
        pautaRequest.setTitle("Pauta de Testes");
        pautaRequest.setDescription("Pauta para testes de votação");

        MvcResult pautaResult = mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pautaRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        PautaResponse pautaResponse = objectMapper.readValue(
                pautaResult.getResponse().getContentAsString(),
                PautaResponse.class);
        Long pautaId = pautaResponse.getId();

        // Open Session
        AbrirSessaoRequest sessaoRequest = new AbrirSessaoRequest();
        sessaoRequest.setDuracaoMinutos(5);

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sessaoRequest)))
                .andExpect(status().isCreated());

        // Register Vote 1
        RegistrarVotoRequest voto1Request = new RegistrarVotoRequest("A1", VotoEnum.SIM);
        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(voto1Request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.associadoId").value("A1"))
                .andExpect(jsonPath("$.voto").value("SIM"));

        // Register Vote 2
        RegistrarVotoRequest voto2Request = new RegistrarVotoRequest("A2", VotoEnum.SIM);
        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(voto2Request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.associadoId").value("A2"))
                .andExpect(jsonPath("$.voto").value("SIM"));

        // Register Vote 3
        RegistrarVotoRequest voto3Request = new RegistrarVotoRequest("A3", VotoEnum.NAO);
        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(voto3Request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.associadoId").value("A3"))
                .andExpect(jsonPath("$.voto").value("NAO"));

        // Get Results
        mockMvc.perform(get("/pautas/{id}/resultado", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pautaId").value(pautaId))
                .andExpect(jsonPath("$.totalVotos").value(3))
                .andExpect(jsonPath("$.votosSim").value(2))
                .andExpect(jsonPath("$.votosNao").value(1))
                .andExpect(jsonPath("$.percentualSim").value(67.0))
                .andExpect(jsonPath("$.percentualNao").value(33.0))
                .andExpect(jsonPath("$.status").value("ATIVA"));

        // Attempt duplicate vote
        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(voto1Request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VOTO_DUPLICADO"));
    }

    @Test
    void integrationTest_VoteWithoutActiveSession_Fails() throws Exception {
        // Create Pauta
        CreatePautaRequest pautaRequest = new CreatePautaRequest();
        pautaRequest.setTitle("Pauta sem Sessão");
        pautaRequest.setDescription("Pauta para testar voto sem sessão");

        MvcResult pautaResult = mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pautaRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        PautaResponse pautaResponse = objectMapper.readValue(
                pautaResult.getResponse().getContentAsString(),
                PautaResponse.class);
        Long pautaId = pautaResponse.getId();

        // Attempt to vote without opening session
        RegistrarVotoRequest votoRequest = new RegistrarVotoRequest("A1", VotoEnum.SIM);
        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(votoRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_ACTIVE"))
                .andExpect(jsonPath("$.message").value("Sessão não está ativa para votação"));
    }

    @Test
    void integrationTest_ResultsWithNoVotes() throws Exception {
        // Create Pauta
        CreatePautaRequest pautaRequest = new CreatePautaRequest();
        pautaRequest.setTitle("Pauta Vazia");
        pautaRequest.setDescription("Pauta sem votos");

        MvcResult pautaResult = mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pautaRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        PautaResponse pautaResponse = objectMapper.readValue(
                pautaResult.getResponse().getContentAsString(),
                PautaResponse.class);
        Long pautaId = pautaResponse.getId();

        // Get results without votes
        mockMvc.perform(get("/pautas/{id}/resultado", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pautaId").value(pautaId))
                .andExpect(jsonPath("$.totalVotos").value(0))
                .andExpect(jsonPath("$.votosSim").value(0))
                .andExpect(jsonPath("$.votosNao").value(0))
                .andExpect(jsonPath("$.percentualSim").value(0.0))
                .andExpect(jsonPath("$.percentualNao").value(0.0))
                .andExpect(jsonPath("$.status").value("SEM_SESSAO"));
    }
}

package br.coop.assembly;

import br.coop.assembly.entity.Pauta;
import br.coop.assembly.entity.Sessao;
import br.coop.assembly.repository.PautaRepository;
import br.coop.assembly.repository.SessaoRepository;
import br.coop.assembly.service.SessaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Sessao Integration Tests")
class SessaoIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PautaRepository pautaRepository;

    @Autowired
    private SessaoRepository sessaoRepository;

    @Autowired
    private SessaoService sessaoService;

    private Pauta testPauta;

    @BeforeEach
    void setUp() {
        sessaoRepository.deleteAll();
        pautaRepository.deleteAll();

        testPauta = new Pauta();
        testPauta.setTitle("Test Pauta");
        testPauta.setDescription("Test Description");
        testPauta.setIsDeleted(false);
        testPauta = pautaRepository.save(testPauta);
    }

    @Test
    @DisplayName("Should complete end-to-end session opening flow")
    void postSessao_EndToEnd_CompletesSuccessfully() throws Exception {
        mockMvc.perform(post("/pautas/" + testPauta.getId() + "/sessao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracaoMinutos\": 5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.pautaId", is(testPauta.getId().intValue())))
                .andExpect(jsonPath("$.status", is("ATIVA")))
                .andExpect(jsonPath("$.dataInicio").exists())
                .andExpect(jsonPath("$.dataFim").exists());

        Sessao persisted = sessaoRepository.findByPautaId(testPauta.getId()).orElseThrow();
        assert persisted != null;
        assert persisted.getStatus() == Sessao.Status.ATIVA;
    }

    @Test
    @DisplayName("Should persist session to database")
    void abrirSessao_PersistsToDatabase() {
        Sessao sessao = sessaoService.abrirSessao(testPauta.getId(), 5);

        Sessao retrieved = sessaoRepository.findById(sessao.getId()).orElseThrow();
        assert retrieved.getPautaId().equals(testPauta.getId());
        assert retrieved.getStatus() == Sessao.Status.ATIVA;
    }

    @Test
    @DisplayName("Should prevent duplicate session opening")
    void postSessao_PreventsDuplicateSessions() throws Exception {
        mockMvc.perform(post("/pautas/" + testPauta.getId() + "/sessao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracaoMinutos\": 5}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/pautas/" + testPauta.getId() + "/sessao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracaoMinutos\": 5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Sessao ja aberta para esta pauta")));
    }

    @Test
    @DisplayName("Should reject opening session when one is already active")
    void abrirSessao_RejectsWhenSessionActive() throws Exception {
        mockMvc.perform(post("/pautas/" + testPauta.getId() + "/sessao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracaoMinutos\": 10}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/pautas/" + testPauta.getId() + "/sessao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracaoMinutos\": 5}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should accept requests without body")
    void postSessao_WithoutBody_UseDefaultDuration() throws Exception {
        mockMvc.perform(post("/pautas/" + testPauta.getId() + "/sessao")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("ATIVA")));
    }

    @Test
    @DisplayName("Should validate duration bounds")
    void postSessao_ValidatesDurationBounds() throws Exception {
        mockMvc.perform(post("/pautas/" + testPauta.getId() + "/sessao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracaoMinutos\": 1441}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/pautas/" + testPauta.getId() + "/sessao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracaoMinutos\": -5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 for non-existent pauta")
    void postSessao_NonExistentPauta_Returns404() throws Exception {
        mockMvc.perform(post("/pautas/9999/sessao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracaoMinutos\": 5}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("PAUTA_NOT_FOUND")));
    }

    @Test
    @DisplayName("Should verify session status transitions")
    void abrirSessao_VerifiesStatusTransitions() {
        Sessao sessao = sessaoService.abrirSessao(testPauta.getId(), 5);

        assert sessaoService.isSessionActive(sessao) : "Session should be active";
        assert sessaoService.getSessionStatus(sessao) == Sessao.Status.ATIVA : "Status should be ATIVA";

        Sessao expiredSessao = new Sessao();
        expiredSessao.setDataFim(LocalDateTime.now().minusMinutes(5));
        assert !sessaoService.isSessionActive(expiredSessao) : "Session should be inactive";
        assert sessaoService.getSessionStatus(expiredSessao) == Sessao.Status.EXPIRADA : "Status should be EXPIRADA";
    }
}

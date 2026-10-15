package br.coop.assembly.controller;

import br.coop.assembly.entity.Pauta;
import br.coop.assembly.entity.Sessao;
import br.coop.assembly.exception.DuracaoInvalidaException;
import br.coop.assembly.exception.PautaNotFoundException;
import br.coop.assembly.exception.SessaoJaAbertaException;
import br.coop.assembly.service.SessaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebMvcTest(SessaoController.class)
@DisplayName("SessaoController Tests")
class SessaoControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SessaoService sessaoService;

    @Autowired
    private ObjectMapper objectMapper;

    private Pauta testPauta;
    private Sessao testSessao;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2024, 1, 15, 10, 0, 0);

        testPauta = new Pauta();
        testPauta.setId(1L);
        testPauta.setTitle("Test Pauta");

        testSessao = new Sessao();
        testSessao.setId(1L);
        testSessao.setPautaId(1L);
        testSessao.setDataInicio(now);
        testSessao.setDataFim(now.plusMinutes(5));
        testSessao.setStatus(Sessao.Status.ATIVA);
    }

    @Test
    @DisplayName("Should open session with valid duration and return 201")
    void postSessao_ComDuracaoValida_Retorna201() throws Exception {
        when(sessaoService.abrirSessao(1L, 5)).thenReturn(testSessao);

        mockMvc.perform(post("/pautas/1/sessao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracaoMinutos\": 5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId", is(1)))
                .andExpect(jsonPath("$.pautaId", is(1)))
                .andExpect(jsonPath("$.status", is("ATIVA")))
                .andExpect(jsonPath("$.mensagem", is("Sessao aberta com sucesso")));
    }

    @Test
    @DisplayName("Should use default duration when body is empty")
    void postSessao_SemCorpo_UsaDuracaoPadrao() throws Exception {
        testSessao.setDataFim(now.plusMinutes(1));
        when(sessaoService.abrirSessao(1L, null)).thenReturn(testSessao);

        mockMvc.perform(post("/pautas/1/sessao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId", is(1)))
                .andExpect(jsonPath("$.pautaId", is(1)));
    }

    @Test
    @DisplayName("Should return 404 when pauta not found")
    void postSessao_PautaInexistente_Retorna404() throws Exception {
        when(sessaoService.abrirSessao(999L, 1)).thenThrow(new PautaNotFoundException(999L));

        mockMvc.perform(post("/pautas/999/sessao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracaoMinutos\": 1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("PAUTA_NOT_FOUND")))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should return 409 when session already open")
    void postSessao_SessaoAtiva_Retorna409() throws Exception {
        when(sessaoService.abrirSessao(1L, 1)).thenThrow(new SessaoJaAbertaException(1L));

        mockMvc.perform(post("/pautas/1/sessao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracaoMinutos\": 1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("SESSAO_JA_ABERTA")))
                .andExpect(jsonPath("$.message", is("Sessao ja aberta para esta pauta")));
    }

    @Test
    @DisplayName("Should return 400 for negative duration")
    void postSessao_DuracaoNegativa_Retorna400() throws Exception {
        when(sessaoService.abrirSessao(1L, -1)).thenThrow(new DuracaoInvalidaException("Duracao deve ser > 0"));

        mockMvc.perform(post("/pautas/1/sessao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracaoMinutos\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("DURACAO_INVALIDA")))
                .andExpect(jsonPath("$.message", containsString("Duracao deve ser > 0")));
    }

    @Test
    @DisplayName("Should return 400 for duration > 1440")
    void postSessao_DuracaoMaiorQue1440_Retorna400() throws Exception {
        when(sessaoService.abrirSessao(1L, 2000)).thenThrow(new DuracaoInvalidaException("Duracao maxima e 1440 minutos"));

        mockMvc.perform(post("/pautas/1/sessao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracaoMinutos\": 2000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("DURACAO_INVALIDA")))
                .andExpect(jsonPath("$.message", containsString("Duracao maxima")));
    }

    @Test
    @DisplayName("Should return 400 for zero duration")
    void postSessao_DuracaoZero_Retorna400() throws Exception {
        when(sessaoService.abrirSessao(1L, 0)).thenThrow(new DuracaoInvalidaException("Duracao deve ser > 0"));

        mockMvc.perform(post("/pautas/1/sessao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracaoMinutos\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("DURACAO_INVALIDA")));
    }

    @Test
    @DisplayName("Should return created location header")
    void postSessao_RetornsLocationHeader() throws Exception {
        when(sessaoService.abrirSessao(1L, 5)).thenReturn(testSessao);

        mockMvc.perform(post("/pautas/1/sessao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracaoMinutos\": 5}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", containsString("/pautas/1/sessao/1")));
    }

    @Test
    @DisplayName("Should include dataInicio and dataFim in response")
    void postSessao_RetornsTimestamps() throws Exception {
        when(sessaoService.abrirSessao(1L, 5)).thenReturn(testSessao);

        mockMvc.perform(post("/pautas/1/sessao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracaoMinutos\": 5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dataInicio").exists())
                .andExpect(jsonPath("$.dataFim").exists());
    }
}

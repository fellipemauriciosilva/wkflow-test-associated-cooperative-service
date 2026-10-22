package com.cooperativa.votacao.api.sessao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cooperativa.votacao.application.pauta.PautaService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SessaoControllerTest {

    private static final Instant T0 = Instant.parse("2026-10-22T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PautaService pautaService;

    @MockBean
    private Clock clock;

    private Long pautaId;

    @BeforeEach
    void setUp() {
        var pauta = pautaService.criar("Eleicao de diretoria " + System.nanoTime(), null);
        pautaId = pauta.getId();
    }

    @Test
    @DisplayName("AC-2.1: POST /pautas/{id}/sessao com duracao informada retorna 2xx com id")
    void abrir_comDuracaoInformada_retorna2xxComId() throws Exception {
        org.mockito.Mockito.when(clock.instant()).thenReturn(T0);

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"duracaoMinutos": 5}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.duracaoMinutos").value(5));
    }

    @Test
    @DisplayName("AC-2.2: duracao omitida assume 1 minuto")
    void abrir_semCorpo_assumeUmMinuto() throws Exception {
        org.mockito.Mockito.when(clock.instant()).thenReturn(T0);

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duracaoMinutos").value(1));
    }

    @Test
    @DisplayName("AC-2.2: duracao null no corpo assume 1 minuto")
    void abrir_comDuracaoNula_assumeUmMinuto() throws Exception {
        org.mockito.Mockito.when(clock.instant()).thenReturn(T0);

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"duracaoMinutos": null}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duracaoMinutos").value(1));
    }

    @Test
    @DisplayName("AC-2.3: duracao zero ou negativa retorna 400")
    void abrir_comDuracaoInvalida_retorna400() throws Exception {
        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"duracaoMinutos": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"duracaoMinutos": -1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Should: duracao nao inteira retorna 400")
    void abrir_comDuracaoNaoInteira_retorna400() throws Exception {
        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"duracaoMinutos": "abc"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"duracaoMinutos": 1.5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("AC-2.4: pauta inexistente retorna 404")
    void abrir_comPautaInexistente_retorna404() throws Exception {
        mockMvc.perform(post("/pautas/{id}/sessao", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"duracaoMinutos": 5}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("AC-2.5: sessao ja aberta para a mesma pauta retorna 409")
    void abrir_comSessaoJaAberta_retorna409() throws Exception {
        org.mockito.Mockito.when(clock.instant())
                .thenReturn(T0)
                .thenReturn(T0.plusSeconds(30));

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"duracaoMinutos": 5}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"duracaoMinutos": 5}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("sessao encerrada nao bloqueia nova abertura para a mesma pauta")
    void abrir_apoSessaoEncerrada_permiteNovaAbertura() throws Exception {
        org.mockito.Mockito.when(clock.instant())
                .thenReturn(T0)
                .thenReturn(T0.plusSeconds(120));

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"duracaoMinutos": 1}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"duracaoMinutos": 1}
                                """))
                .andExpect(status().isCreated());
    }
}

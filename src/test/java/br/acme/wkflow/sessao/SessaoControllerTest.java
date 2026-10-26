package br.acme.wkflow.sessao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.acme.wkflow.pauta.Pauta;
import br.acme.wkflow.pauta.PautaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SessaoControllerTest {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final Instant AGORA = Instant.parse("2026-10-26T12:00:00Z");

    @TestConfiguration
    static class RelogioFixo {

        @Bean
        @Primary
        Clock relogioFixo() {
            return Clock.fixed(AGORA, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PautaRepository pautas;

    @Autowired
    private SessaoRepository sessoes;

    private Long pautaId;

    @BeforeEach
    void criarPauta() {
        pautaId = pautas.save(new Pauta("Aprovar contas")).getId();
    }

    @Test
    void ac21_postAbreSessaoParaPautaExistenteERetorna201() throws Exception {
        mvc.perform(post("/pautas/{id}/sessao", pautaId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pautaId").value(pautaId))
                .andExpect(jsonPath("$.inicio").isNotEmpty())
                .andExpect(jsonPath("$.fim").isNotEmpty());

        assertEquals(1L, sessoes.findAll().stream().filter(s -> pautaId.equals(s.getPautaId())).count());
    }

    @Test
    void ac22_semDuracaoSessaoDuraUmMinuto() throws Exception {
        mvc.perform(post("/pautas/{id}/sessao", pautaId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inicio").value(AGORA.toString()))
                .andExpect(jsonPath("$.fim").value(AGORA.plus(Duration.ofMinutes(1)).toString()));
    }

    @Test
    void ac22_duracaoNulaExplicitaSessaoDuraUmMinuto() throws Exception {
        mvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\": null}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inicio").value(AGORA.toString()))
                .andExpect(jsonPath("$.fim").value(AGORA.plus(Duration.ofMinutes(1)).toString()));
    }

    @Test
    void ac23_comDuracaoSessaoDuraOValorEmMinutos() throws Exception {
        mvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\": 5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inicio").value(AGORA.toString()))
                .andExpect(jsonPath("$.fim").value(AGORA.plus(Duration.ofMinutes(5)).toString()));
    }

    @Test
    void ac24_pautaInexistenteRetorna404EmProblemDetail() throws Exception {
        mvc.perform(post("/pautas/{id}/sessao", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void ac25_duracaoZeroOuNegativaRetorna400() throws Exception {
        mvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));

        mvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void ac26_duracaoNaoInteiraRetorna400() throws Exception {
        mvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\": \"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void ac26_duracaoFracionariaRetorna400() throws Exception {
        mvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\": 5.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void ac27_reaberturaComSessaoAindaAbertaRetorna409() throws Exception {
        sessoes.save(new Sessao(pautaId, AGORA.minus(Duration.ofMinutes(1)), AGORA.plus(Duration.ofMinutes(5))));

        mvc.perform(post("/pautas/{id}/sessao", pautaId))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));

        assertEquals(1L, sessoes.findAll().stream().filter(s -> pautaId.equals(s.getPautaId())).count());
    }

    @Test
    void ac27_reaberturaAposSessaoExpiradaRetorna201() throws Exception {
        sessoes.save(new Sessao(pautaId, AGORA.minus(Duration.ofMinutes(10)), AGORA.minus(Duration.ofMinutes(5))));

        mvc.perform(post("/pautas/{id}/sessao", pautaId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inicio").value(AGORA.toString()));

        assertEquals(2L, sessoes.findAll().stream().filter(s -> pautaId.equals(s.getPautaId())).count());
    }

    @Test
    void ac26_duracaoStringNumericaRetorna400() throws Exception {
        mvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\": \"5\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }
}

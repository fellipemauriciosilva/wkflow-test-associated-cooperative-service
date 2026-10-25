package br.acme.pauta;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SessaoControllerTest.RelogioDeTeste.class)
class SessaoControllerTest {

    private static final Instant T0 = Instant.parse("2026-01-01T12:00:00Z");

    @TestConfiguration
    static class RelogioDeTeste {
        @Bean
        @Primary
        MutableClock relogioTeste() {
            return new MutableClock(T0);
        }
    }

    static final class MutableClock extends Clock {
        private final AtomicReference<Instant> agora;

        MutableClock(Instant inicial) {
            this.agora = new AtomicReference<>(inicial);
        }

        void avancarPara(Instant instante) {
            agora.set(instante);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora.get();
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MutableClock relogio;

    @Autowired
    private SessaoRepository sessoes;

    @BeforeEach
    void relogioNoInicio() {
        relogio.avancarPara(T0);
    }

    @Test
    void abrirSessaoComDuracao_gravaInicioEFimComDuracaoInformada() throws Exception {
        long pautaId = criarPauta("Duracao informada");

        abrirSessao(pautaId, "{\"duracaoMinutos\": 5}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pautaId").value(pautaId))
                .andExpect(jsonPath("$.inicio").value(T0.toString()))
                .andExpect(jsonPath("$.fim").value(T0.plusSeconds(300).toString()))
                .andExpect(jsonPath("$.duracaoMinutos").value(5));

        Sessao persistida = sessoes.findByPautaId(pautaId).get(0);
        assertThat(persistida.getInicio()).isEqualTo(T0);
        assertThat(persistida.getFim()).isEqualTo(T0.plusSeconds(300));
    }

    @Test
    void abrirSessaoSemCorpo_usaDuracaoPadraoDeUmMinuto() throws Exception {
        long pautaId = criarPauta("Sem corpo");

        abrirSessao(pautaId, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fim").value(T0.plusSeconds(60).toString()))
                .andExpect(jsonPath("$.duracaoMinutos").value(1));
    }

    @Test
    void abrirSessaoSemCampoDuracao_usaDuracaoPadraoDeUmMinuto() throws Exception {
        long pautaId = criarPauta("Sem campo");

        abrirSessao(pautaId, "{}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fim").value(T0.plusSeconds(60).toString()));
    }

    @Test
    void abrirSessaoComDuracaoDez_respeitaValorInformado() throws Exception {
        long pautaId = criarPauta("Dez minutos");

        abrirSessao(pautaId, "{\"duracaoMinutos\": 10}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fim").value(T0.plusSeconds(600).toString()))
                .andExpect(jsonPath("$.duracaoMinutos").value(10));
    }

    @Test
    void abrirSessaoEmPautaInexistente_retorna404FormatoPadrao() throws Exception {
        abrirSessao(999999L, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(containsString("999999")))
                .andExpect(jsonPath("$.path").value("/pautas/999999/sessao"));
    }

    @Test
    void abrirSessaoDuplicadaComSessaoAberta_retorna409SemCriarNova() throws Exception {
        long pautaId = criarPauta("Duplicada");
        abrirSessao(pautaId, "{\"duracaoMinutos\": 5}").andExpect(status().isCreated());

        relogio.avancarPara(T0.plusSeconds(60));
        abrirSessao(pautaId, "{\"duracaoMinutos\": 5}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.path").value("/pautas/" + pautaId + "/sessao"));

        assertThat(sessoes.findByPautaId(pautaId)).hasSize(1);
    }

    @Test
    void abrirSessaoAposFimDaAnterior_retorna201() throws Exception {
        long pautaId = criarPauta("Encerrada");
        abrirSessao(pautaId, "{\"duracaoMinutos\": 1}").andExpect(status().isCreated());

        relogio.avancarPara(T0.plusSeconds(60));
        abrirSessao(pautaId, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inicio").value(T0.plusSeconds(60).toString()));

        assertThat(sessoes.findByPautaId(pautaId)).hasSize(2);
    }

    @Test
    void abrirSessaoComDuracaoZero_retorna400CitandoCampo() throws Exception {
        long pautaId = criarPauta("Duracao zero");

        abrirSessao(pautaId, "{\"duracaoMinutos\": 0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("duracaoMinutos")));
    }

    @Test
    void abrirSessaoComDuracaoNegativa_retorna400CitandoCampo() throws Exception {
        long pautaId = criarPauta("Duracao negativa");

        abrirSessao(pautaId, "{\"duracaoMinutos\": -3}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("duracaoMinutos")));
    }

    @Test
    void abrirSessaoComDuracaoFracionaria_retorna400CitandoCampo() throws Exception {
        long pautaId = criarPauta("Duracao fracionaria");

        abrirSessao(pautaId, "{\"duracaoMinutos\": 1.5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("duracaoMinutos")));
    }

    @Test
    void sessaoComFimIgualAoAgora_nao_contaComoAberta() throws Exception {
        long pautaId = criarPauta("Limite fim exclusivo");
        abrirSessao(pautaId, "{\"duracaoMinutos\": 1}").andExpect(status().isCreated());

        relogio.avancarPara(T0.plusSeconds(60));
        abrirSessao(pautaId, "{\"duracaoMinutos\": 1}").andExpect(status().isCreated());
    }

    private long criarPauta(String titulo) throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"" + titulo + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(criada.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private ResultActions abrirSessao(long pautaId, String corpo) throws Exception {
        var requisicao = post("/pautas/{id}/sessao", pautaId);
        if (corpo != null) {
            requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        return mvc.perform(requisicao);
    }

    private ResultActions abrirSessao(Long pautaId, String corpo) throws Exception {
        return abrirSessao(pautaId.longValue(), corpo);
    }
}

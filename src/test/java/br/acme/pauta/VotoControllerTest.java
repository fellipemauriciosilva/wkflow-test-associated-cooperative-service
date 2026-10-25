package br.acme.pauta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:voto;DB_CLOSE_DELAY=-1")
class VotoControllerTest {

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
        private volatile String threadPausada;
        private volatile CountDownLatch chegou;
        private volatile CountDownLatch liberar;

        MutableClock(Instant inicial) {
            this.agora = new AtomicReference<>(inicial);
        }

        void avancarPara(Instant instante) {
            agora.set(instante);
        }

        void pausarAoLerAgora(String nomeDaThread, CountDownLatch chegou, CountDownLatch liberar) {
            this.chegou = chegou;
            this.liberar = liberar;
            this.threadPausada = nomeDaThread;
        }

        void retomar() {
            threadPausada = null;
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
            Instant valor = agora.get();
            if (Thread.currentThread().getName().equals(threadPausada)) {
                chegou.countDown();
                try {
                    liberar.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return valor;
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MutableClock relogio;

    @Autowired
    private VotoRepository votos;

    @BeforeEach
    void relogioNoInicio() {
        relogio.retomar();
        relogio.avancarPara(T0);
    }

    @Test
    void votoSimComSessaoAbertaRetorna201EPersiste() throws Exception {
        long pautaId = criarPauta("Voto SIM");
        abrirSessao(pautaId, "{\"duracaoMinutos\": 5}").andExpect(status().isCreated());

        votar(pautaId, "{\"associadoId\": \"a1\", \"opcao\": \"SIM\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.pautaId").value(pautaId))
                .andExpect(jsonPath("$.sessaoId").exists())
                .andExpect(jsonPath("$.associadoId").value("a1"))
                .andExpect(jsonPath("$.opcao").value("SIM"))
                .andExpect(jsonPath("$.criadoEm").value(T0.toString()));

        assertThat(votos.findByPautaId(pautaId)).hasSize(1);
    }

    @Test
    void votoNaoComSessaoAbertaRetorna201() throws Exception {
        long pautaId = criarPauta("Voto NAO");
        abrirSessao(pautaId, "{\"duracaoMinutos\": 5}").andExpect(status().isCreated());

        votar(pautaId, "{\"associadoId\": \"a1\", \"opcao\": \"NAO\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.opcao").value("NAO"));
    }

    @Test
    void opcaoForaDeSimOuNaoRetorna400() throws Exception {
        long pautaId = criarPauta("Opcao invalida");
        abrirSessao(pautaId, "{\"duracaoMinutos\": 5}").andExpect(status().isCreated());

        votar(pautaId, "{\"associadoId\": \"a1\", \"opcao\": \"TALVEZ\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("opcao")));
        votar(pautaId, "{\"associadoId\": \"a1\", \"opcao\": \"sim\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("opcao")));

        assertThat(votos.findByPautaId(pautaId)).isEmpty();
    }

    @Test
    void associadoIdAusenteRetorna400CitandoCampo() throws Exception {
        long pautaId = criarPauta("Sem associado");
        abrirSessao(pautaId, "{\"duracaoMinutos\": 5}").andExpect(status().isCreated());

        votar(pautaId, "{\"opcao\": \"SIM\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("associadoId")));
    }

    @Test
    void votoEmPautaInexistenteRetorna404FormatoPadrao() throws Exception {
        votar(999999L, "{\"associadoId\": \"a1\", \"opcao\": \"SIM\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/pautas/999999/votos"));
    }

    @Test
    void votoSemSessaoNuncaAbertaRetorna409() throws Exception {
        long pautaId = criarPauta("Sem sessao");

        votar(pautaId, "{\"associadoId\": \"a1\", \"opcao\": \"SIM\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.path").value("/pautas/" + pautaId + "/votos"));

        assertThat(votos.findByPautaId(pautaId)).isEmpty();
    }

    @Test
    void votoAposEncerramentoDaSessaoRetorna409() throws Exception {
        long pautaId = criarPauta("Encerrada");
        abrirSessao(pautaId, "{\"duracaoMinutos\": 1}").andExpect(status().isCreated());

        relogio.avancarPara(T0.plusSeconds(120));
        votar(pautaId, "{\"associadoId\": \"a1\", \"opcao\": \"SIM\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void votoNoInstanteDoFimDaSessaoRetorna409() throws Exception {
        long pautaId = criarPauta("Fim exclusivo");
        abrirSessao(pautaId, "{\"duracaoMinutos\": 1}").andExpect(status().isCreated());

        relogio.avancarPara(T0.plusSeconds(60));
        votar(pautaId, "{\"associadoId\": \"a1\", \"opcao\": \"SIM\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void segundoVotoDoMesmoAssociadoNaPautaRetorna409() throws Exception {
        long pautaId = criarPauta("Duplicado");
        abrirSessao(pautaId, "{\"duracaoMinutos\": 5}").andExpect(status().isCreated());

        votar(pautaId, "{\"associadoId\": \"a1\", \"opcao\": \"SIM\"}").andExpect(status().isCreated());
        votar(pautaId, "{\"associadoId\": \"a1\", \"opcao\": \"NAO\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));

        assertThat(votos.findByPautaId(pautaId)).hasSize(1);
    }

    @Test
    void votoConcorrenteDoMesmoAssociadoUmCriaOutroRecebe409() throws Exception {
        long pautaId = criarPauta("Concorrente");
        abrirSessao(pautaId, "{\"duracaoMinutos\": 5}").andExpect(status().isCreated());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> resultados = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                resultados.add(pool.submit(() -> {
                    largada.await();
                    return votar(pautaId, "{\"associadoId\": \"a1\", \"opcao\": \"SIM\"}")
                            .andReturn().getResponse().getStatus();
                }));
            }
            largada.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> resultado : resultados) {
                statuses.add(resultado.get(30, TimeUnit.SECONDS));
            }
            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        } finally {
            pool.shutdownNow();
        }

        assertThat(votos.findByPautaId(pautaId)).hasSize(1);
    }

    @Test
    void resultadoEsperaVotoEmVooAntesDeApurar() throws Exception {
        long pautaId = criarPauta("Voto em voo");
        abrirSessao(pautaId, "{\"duracaoMinutos\": 5}").andExpect(status().isCreated());

        CountDownLatch votoLeuAgora = new CountDownLatch(1);
        CountDownLatch liberarCommit = new CountDownLatch(1);
        relogio.pausarAoLerAgora("voto-em-voo", votoLeuAgora, liberarCommit);
        ExecutorService votante = Executors.newSingleThreadExecutor(r -> new Thread(r, "voto-em-voo"));
        ExecutorService consulta = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> voto = votante.submit(() ->
                    votar(pautaId, "{\"associadoId\": \"a1\", \"opcao\": \"SIM\"}")
                            .andReturn().getResponse().getStatus());
            assertThat(votoLeuAgora.await(30, TimeUnit.SECONDS)).isTrue();

            relogio.avancarPara(T0.plusSeconds(300));
            Future<String> leitura = consulta.submit(() ->
                    resultado(pautaId).andReturn().getResponse().getContentAsString());
            Thread.sleep(300);
            assertThat(leitura.isDone()).as("resultado deve esperar o voto em voo").isFalse();

            liberarCommit.countDown();
            assertThat(voto.get(30, TimeUnit.SECONDS)).isEqualTo(201);
            assertThat(leitura.get(30, TimeUnit.SECONDS)).contains("\"sim\":1").contains("\"total\":1");
        } finally {
            relogio.retomar();
            liberarCommit.countDown();
            votante.shutdownNow();
            consulta.shutdownNow();
        }
    }

    @Test
    void resultadoAposEncerramentoApuraSimENaoETotal() throws Exception {
        long pautaId = criarPauta("Apuracao");
        abrirSessao(pautaId, "{\"duracaoMinutos\": 5}").andExpect(status().isCreated());
        votar(pautaId, "{\"associadoId\": \"a1\", \"opcao\": \"SIM\"}").andExpect(status().isCreated());
        votar(pautaId, "{\"associadoId\": \"a2\", \"opcao\": \"SIM\"}").andExpect(status().isCreated());
        votar(pautaId, "{\"associadoId\": \"a3\", \"opcao\": \"NAO\"}").andExpect(status().isCreated());

        relogio.avancarPara(T0.plusSeconds(300));
        resultado(pautaId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sim").value(2))
                .andExpect(jsonPath("$.nao").value(1))
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void resultadoDePautaSemVotosRetornaZeros() throws Exception {
        long pautaId = criarPauta("Sem votos");
        abrirSessao(pautaId, "{\"duracaoMinutos\": 1}").andExpect(status().isCreated());

        relogio.avancarPara(T0.plusSeconds(60));
        resultado(pautaId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sim").value(0))
                .andExpect(jsonPath("$.nao").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void resultadoComSessaoAbertaRetorna409() throws Exception {
        long pautaId = criarPauta("Resultado parcial");
        abrirSessao(pautaId, "{\"duracaoMinutos\": 5}").andExpect(status().isCreated());
        votar(pautaId, "{\"associadoId\": \"a1\", \"opcao\": \"SIM\"}").andExpect(status().isCreated());

        resultado(pautaId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.path").value("/pautas/" + pautaId + "/resultado"));
    }

    @Test
    void resultadoDePautaInexistenteRetorna404() throws Exception {
        resultado(999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void associadoIdVazioOuEmBrancoRetorna400CitandoCampo() throws Exception {
        long pautaId = criarPauta("Associado vazio");
        abrirSessao(pautaId, "{\"duracaoMinutos\": 5}").andExpect(status().isCreated());

        for (String associado : List.of("", "   ")) {
            votar(pautaId, "{\"associadoId\": \"" + associado + "\", \"opcao\": \"SIM\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message").value(containsString("associadoId")));
        }
        assertThat(votos.findByPautaId(pautaId)).isEmpty();
    }

    @Test
    void associadoIdComDuzentosECincoentaESeisCaracteresRetorna400CitandoCampo() throws Exception {
        long pautaId = criarPauta("Associado longo");
        abrirSessao(pautaId, "{\"duracaoMinutos\": 5}").andExpect(status().isCreated());

        votar(pautaId, "{\"associadoId\": \"" + "a".repeat(256) + "\", \"opcao\": \"SIM\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("associadoId")));
        assertThat(votos.findByPautaId(pautaId)).isEmpty();
    }

    @Test
    void bodyInvalidoEmPautaInexistenteRetorna400AntesDe404() throws Exception {
        votar(999999L, "{\"associadoId\": \"\", \"opcao\": \"SIM\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void associadoJaVotadoEmPautaSemSessaoRetornaErroDeSessaoAntesDeDuplicado() throws Exception {
        long pautaId = criarPauta("Ordem sessao");
        abrirSessao(pautaId, "{\"duracaoMinutos\": 1}").andExpect(status().isCreated());
        votar(pautaId, "{\"associadoId\": \"a1\", \"opcao\": \"SIM\"}").andExpect(status().isCreated());

        relogio.avancarPara(T0.plusSeconds(60));
        votar(pautaId, "{\"associadoId\": \"a1\", \"opcao\": \"SIM\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("sem sessao aberta")));
    }

    @Test
    void associadoIdComEspacoOuCaixaDiferenteNaoVotaDeNovo() throws Exception {
        long pautaId = criarPauta("Normalizacao");
        abrirSessao(pautaId, "{\"duracaoMinutos\": 5}").andExpect(status().isCreated());

        votar(pautaId, "{\"associadoId\": \"a1\", \"opcao\": \"SIM\"}").andExpect(status().isCreated());
        votar(pautaId, "{\"associadoId\": \"a1 \", \"opcao\": \"NAO\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
        votar(pautaId, "{\"associadoId\": \"A1\", \"opcao\": \"NAO\"}")
                .andExpect(status().isConflict());

        assertThat(votos.findByPautaId(pautaId))
                .singleElement()
                .satisfies(voto -> assertThat(voto.getAssociadoId()).isEqualTo("a1"));
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
        return mvc.perform(post("/pautas/{id}/sessao", pautaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private ResultActions votar(long pautaId, String corpo) throws Exception {
        return mvc.perform(post("/pautas/{id}/votos", pautaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private ResultActions resultado(long pautaId) throws Exception {
        return mvc.perform(get("/pautas/{id}/resultado", pautaId));
    }
}

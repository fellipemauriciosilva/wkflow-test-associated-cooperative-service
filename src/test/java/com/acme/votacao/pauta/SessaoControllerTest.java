package com.acme.votacao.pauta;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestClockConfig.class, TestGanchoConcorrenciaConfig.class})
class SessaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PautaRepository pautaRepository;

    @Autowired
    private SessaoRepository sessaoRepository;

    @Autowired
    private MutableClock clock;

    @Autowired
    private GanchoConcorrenciaSessaoControlavel gancho;

    private Long pautaId;

    @BeforeEach
    void setUp() {
        clock.definir(Instant.parse("2026-01-01T10:00:00Z"));
        Pauta pauta = pautaRepository.save(new Pauta("Reforma do estatuto", "Discussão sobre regras internas"));
        pautaId = pauta.getId();
    }

    @Test
    @DisplayName("POST /pautas/{id}/sessao sem body abre sessão com duração padrão de 1 minuto e retorna 201")
    void abrir_semBody_retorna201ComDuracaoPadrao() throws Exception {
        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.pautaId").value(pautaId))
                .andExpect(jsonPath("$.duracaoMinutos").value(1))
                .andExpect(jsonPath("$.inicio").value("2026-01-01T10:00:00Z"))
                .andExpect(jsonPath("$.fim").value("2026-01-01T10:01:00Z"));
    }

    @Test
    @DisplayName("POST /pautas/{id}/sessao com corpo vazio {} abre sessão com duração padrão")
    void abrir_comCorpoVazio_retorna201ComDuracaoPadrao() throws Exception {
        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duracaoMinutos").value(1));
    }

    @Test
    @DisplayName("POST /pautas/{id}/sessao com duracaoMinutos informada usa essa duração")
    void abrir_comDuracao_retorna201ComDuracaoInformada() throws Exception {
        String payload = """
                {"duracaoMinutos": 5}
                """;

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duracaoMinutos").value(5))
                .andExpect(jsonPath("$.inicio").value("2026-01-01T10:00:00Z"))
                .andExpect(jsonPath("$.fim").value("2026-01-01T10:05:00Z"));
    }

    @Test
    @DisplayName("POST /pautas/{id}/sessao com duracaoMinutos: null trata como omitida")
    void abrir_comDuracaoNula_retorna201ComDuracaoPadrao() throws Exception {
        String payload = """
                {"duracaoMinutos": null}
                """;

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duracaoMinutos").value(1));
    }

    @Test
    @DisplayName("POST /pautas/{id}/sessao repetido enquanto sessão está aberta retorna 409 com corpo padronizado")
    void abrir_sessaoJaAberta_retorna409() throws Exception {
        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        clock.definir(Instant.parse("2026-01-01T10:00:30Z"));

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Já existe sessão aberta para a pauta: " + pautaId))
                .andExpect(jsonPath("$.timestamp").value("2026-01-01T10:00:30Z"))
                .andExpect(jsonPath("$.path").value("/pautas/" + pautaId + "/sessao"));
    }

    @Test
    @DisplayName("ErroResponse.timestamp vem do Clock injetado, não do relógio real")
    void erro_timestamp_vemDoClockInjetado() throws Exception {
        clock.definir(Instant.parse("2030-05-20T15:30:00Z"));

        mockMvc.perform(post("/pautas/{id}/sessao", 999999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").value("2030-05-20T15:30:00Z"));
    }

    @Test
    @DisplayName("POST /pautas/{id}/sessao para pauta inexistente retorna 404 com corpo padronizado")
    void abrir_pautaInexistente_retorna404() throws Exception {
        mockMvc.perform(post("/pautas/{id}/sessao", 999999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/pautas/999999/sessao"));
    }

    @Test
    @DisplayName("POST /pautas/{id}/sessao com duracaoMinutos zero retorna 400 e não cria sessão")
    void abrir_duracaoZero_retorna400ENaoCriaSessao() throws Exception {
        String payload = """
                {"duracaoMinutos": 0}
                """;

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        assertThat(sessaoRepository.countByPautaId(pautaId)).isZero();
    }

    @Test
    @DisplayName("POST /pautas/{id}/sessao com duracaoMinutos negativa retorna 400 e não cria sessão")
    void abrir_duracaoNegativa_retorna400ENaoCriaSessao() throws Exception {
        String payload = """
                {"duracaoMinutos": -1}
                """;

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        assertThat(sessaoRepository.countByPautaId(pautaId)).isZero();
    }

    @Test
    @DisplayName("POST /pautas/{id}/sessao com duracaoMinutos não inteira (1.5) retorna 400 e não cria sessão")
    void abrir_duracaoNaoInteira_retorna400ENaoCriaSessao() throws Exception {
        String payload = """
                {"duracaoMinutos": 1.5}
                """;

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        assertThat(sessaoRepository.countByPautaId(pautaId)).isZero();
    }

    @Test
    @DisplayName("POST /pautas/{id}/sessao com duracaoMinutos de tipo inválido (string) retorna 400 e não cria sessão")
    void abrir_duracaoTipoInvalido_retorna400ENaoCriaSessao() throws Exception {
        String payload = """
                {"duracaoMinutos": "abc"}
                """;

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        assertThat(sessaoRepository.countByPautaId(pautaId)).isZero();
    }

    @Test
    @DisplayName("POST /pautas/{id}/sessao com duracaoMinutos acima do teto (Integer.MAX_VALUE) retorna 400 e não cria sessão")
    void abrir_duracaoAcimaDoTeto_retorna400ENaoCriaSessao() throws Exception {
        String payload = """
                {"duracaoMinutos": 2147483647}
                """;

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        assertThat(sessaoRepository.countByPautaId(pautaId)).isZero();
    }

    @Test
    @DisplayName("POST /pautas/{id}/sessao com duracaoMinutos no teto (1440) retorna 201")
    void abrir_duracaoNoTeto_retorna201() throws Exception {
        String payload = """
                {"duracaoMinutos": 1440}
                """;

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duracaoMinutos").value(1440));
    }

    @Test
    @DisplayName("POST /pautas/{id}/sessao com duracaoMinutos acima do teto por pouco (1441) retorna 400")
    void abrir_duracaoAcimaDoTetoPorPouco_retorna400() throws Exception {
        String payload = """
                {"duracaoMinutos": 1441}
                """;

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        assertThat(sessaoRepository.countByPautaId(pautaId)).isZero();
    }

    @Test
    @DisplayName("POST com Content-Type não suportado retorna 415 com corpo padronizado")
    void abrir_tipoMidiaNaoSuportado_retorna415ComCorpoPadronizado() throws Exception {
        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("duracaoMinutos=5"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.path").value("/pautas/" + pautaId + "/sessao"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Método HTTP não suportado na rota retorna 405 com corpo padronizado")
    void abrir_metodoNaoSuportado_retorna405ComCorpoPadronizado() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/pautas/{id}/sessao", pautaId))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"))
                .andExpect(jsonPath("$.path").value("/pautas/" + pautaId + "/sessao"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Rota inexistente retorna 404 com corpo padronizado")
    void rotaInexistente_retorna404ComCorpoPadronizado() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/rota-que-nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/rota-que-nao-existe"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Erro não tratado (RuntimeException) retorna 500 com corpo padronizado e sem texto da exceção")
    void abrir_erroNaoTratado_retorna500ComCorpoPadronizadoSemTextoDaExcecao() throws Exception {
        gancho.lancarErroNaProximaChamada();

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("erro interno inesperado"))
                .andExpect(jsonPath("$.path").value("/pautas/" + pautaId + "/sessao"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("RuntimeException"))));

        assertThat(sessaoRepository.countByPautaId(pautaId)).isZero();
    }

    @Test
    @DisplayName("POST /pautas/{id}/sessao após fim da sessão anterior permite reabertura com 201")
    void abrir_sessaoAnteriorEncerrada_permiteReaberturaCom201() throws Exception {
        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        clock.definir(Instant.parse("2026-01-01T10:05:00Z"));

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inicio").value("2026-01-01T10:05:00Z"));
    }

    @Test
    @DisplayName("duas aberturas concorrentes para a mesma pauta: exatamente uma sucede com 201, a outra recebe 409, e a segunda fica bloqueada até a primeira liberar o lock")
    void abrir_duasChamadasConcorrentes_exatamenteUmaSucede() throws Exception {
        gancho.reset();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> tarefa1 = executor.submit(() -> abrirChamando());
            gancho.aguardarPrimeiraChegada();

            assertThat(gancho.nivelIsolamentoObservado())
                    .as("isolamento efetivo da transação de SessaoService.abrir deve ser READ_COMMITTED")
                    .isEqualTo(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);

            Future<Integer> tarefa2 = executor.submit(() -> abrirChamando());

            // Tarefa1 está retida no gancho, dentro da transação, com o lock pessimista
            // ainda tomado. Se o lock não bloqueasse a segunda transação, ela chegaria ao
            // gancho mesmo com a primeira retida — então esta asserção falha sem o lock.
            Thread.sleep(200);
            assertThat(gancho.segundaChegou())
                    .as("segunda transação não deveria alcançar o gancho enquanto a primeira está retida com o lock")
                    .isFalse();

            gancho.liberarPrimeira();

            int status1 = tarefa1.get(10, TimeUnit.SECONDS);
            int status2 = tarefa2.get(10, TimeUnit.SECONDS);

            AtomicInteger sucessos = new AtomicInteger();
            AtomicInteger conflitos = new AtomicInteger();
            for (int status : new int[]{status1, status2}) {
                if (status == 201) {
                    sucessos.incrementAndGet();
                } else if (status == 409) {
                    conflitos.incrementAndGet();
                }
            }

            assertThat(sucessos.get()).isEqualTo(1);
            assertThat(conflitos.get()).isEqualTo(1);
            assertThat(sessaoRepository.countByPautaId(pautaId)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private int abrirChamando() {
        try {
            return mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

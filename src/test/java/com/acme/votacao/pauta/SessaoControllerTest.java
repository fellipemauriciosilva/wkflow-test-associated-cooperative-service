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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
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
    @DisplayName("POST /pautas/{id}/sessao repetido enquanto sessão está aberta retorna 409")
    void abrir_sessaoJaAberta_retorna409() throws Exception {
        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/pautas/" + pautaId + "/sessao"));
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
    @DisplayName("duas aberturas concorrentes para a mesma pauta: exatamente uma sucede com 201, a outra recebe 409")
    void abrir_duasChamadasConcorrentes_exatamenteUmaSucede() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);

        Future<Integer> tarefa1 = executor.submit(() -> abrirAguardandoLargada(largada));
        Future<Integer> tarefa2 = executor.submit(() -> abrirAguardandoLargada(largada));

        largada.countDown();

        int status1 = tarefa1.get(10, TimeUnit.SECONDS);
        int status2 = tarefa2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

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
    }

    private int abrirAguardandoLargada(CountDownLatch largada) {
        try {
            largada.await();
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

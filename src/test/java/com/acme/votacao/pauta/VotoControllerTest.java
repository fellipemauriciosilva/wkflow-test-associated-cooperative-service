package com.acme.votacao.pauta;

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
@Import({TestClockConfig.class, TestGanchoConcorrenciaVotoConfig.class})
class VotoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PautaRepository pautaRepository;

    @Autowired
    private SessaoRepository sessaoRepository;

    @Autowired
    private VotoRepository votoRepository;

    @Autowired
    private MutableClock clock;

    @Autowired
    private GanchoConcorrenciaVotoControlavel gancho;

    private Long pautaId;

    @BeforeEach
    void setUp() {
        clock.definir(Instant.parse("2026-01-01T10:00:00Z"));
        Pauta pauta = pautaRepository.save(new Pauta("Reforma do estatuto", "Discussão sobre regras internas"));
        pautaId = pauta.getId();
    }

    private void abrirSessao(int duracaoMinutos) {
        Instant agora = Instant.parse("2026-01-01T10:00:00Z");
        sessaoRepository.save(new Sessao(pautaId, agora, agora.plusSeconds(60L * duracaoMinutos), duracaoMinutos));
    }

    @Test
    @DisplayName("AC-3.1: voto SIM com sessão aberta retorna 201 com corpo completo")
    void votar_sessaoAbertaVotoSim_retorna201() throws Exception {
        abrirSessao(5);
        String payload = """
                {"associadoId": "associado-1", "voto": "SIM"}
                """;

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.pautaId").value(pautaId))
                .andExpect(jsonPath("$.associadoId").value("associado-1"))
                .andExpect(jsonPath("$.voto").value("SIM"));
    }

    @Test
    @DisplayName("AC-3.1: voto NAO de outro associado com sessão aberta retorna 201")
    void votar_sessaoAbertaVotoNao_retorna201() throws Exception {
        abrirSessao(5);
        String payload = """
                {"associadoId": "associado-2", "voto": "NAO"}
                """;

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.voto").value("NAO"));
    }

    @Test
    @DisplayName("AC-3.2: segundo voto do mesmo associado na mesma pauta retorna 409")
    void votar_associadoJaVotou_retorna409() throws Exception {
        abrirSessao(5);
        String payload = """
                {"associadoId": "associado-1", "voto": "SIM"}
                """;

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Associado associado-1 já votou na pauta: " + pautaId))
                .andExpect(jsonPath("$.path").value("/pautas/" + pautaId + "/votos"));
    }

    @Test
    @DisplayName("AC-3.3: votar com sessão fechada (Clock avançado além do fim) retorna 422")
    void votar_sessaoFechada_retorna422() throws Exception {
        abrirSessao(1);
        clock.definir(Instant.parse("2026-01-01T10:01:30Z"));
        String payload = """
                {"associadoId": "associado-1", "voto": "SIM"}
                """;

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Sessão não está aberta para votação na pauta: " + pautaId));

        assertThat(votoRepository.countByPautaId(pautaId)).isZero();
    }

    @Test
    @DisplayName("AC-3.3: votar em pauta sem sessão retorna 422")
    void votar_pautaSemSessao_retorna422() throws Exception {
        String payload = """
                {"associadoId": "associado-1", "voto": "SIM"}
                """;

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity());

        assertThat(votoRepository.countByPautaId(pautaId)).isZero();
    }

    @Test
    @DisplayName("Ordem 422 antes de 409: associado já votado e sessão fechada retorna 422, não 409")
    void votar_associadoJaVotadoComSessaoFechada_retorna422EmVezDe409() throws Exception {
        abrirSessao(1);
        String payload = """
                {"associadoId": "associado-1", "voto": "SIM"}
                """;

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        clock.definir(Instant.parse("2026-01-01T10:01:30Z"));

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));

        assertThat(votoRepository.countByPautaId(pautaId)).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-3.4: voto fora de SIM/NAO retorna 400")
    void votar_valorInvalido_retorna400() throws Exception {
        abrirSessao(5);
        String payload = """
                {"associadoId": "associado-1", "voto": "TALVEZ"}
                """;

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("AC-3.4: voto ausente retorna 400")
    void votar_votoAusente_retorna400() throws Exception {
        abrirSessao(5);
        String payload = """
                {"associadoId": "associado-1"}
                """;

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("AC-3.6: POST /pautas/{id}/votos com pauta inexistente retorna 404")
    void votar_pautaInexistente_retorna404() throws Exception {
        String payload = """
                {"associadoId": "associado-1", "voto": "SIM"}
                """;

        mockMvc.perform(post("/pautas/{id}/votos", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("AC-3.8: votar com agora igual ao fim da sessão retorna 422 (fronteira exclusiva)")
    void votar_agoraIgualAoFim_retorna422() throws Exception {
        abrirSessao(1);
        clock.definir(Instant.parse("2026-01-01T10:01:00Z"));
        String payload = """
                {"associadoId": "associado-1", "voto": "SIM"}
                """;

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("AC-3.9: associadoId ausente retorna 400")
    void votar_associadoIdAusente_retorna400() throws Exception {
        abrirSessao(5);
        String payload = """
                {"voto": "SIM"}
                """;

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("AC-3.9: associadoId em branco retorna 400")
    void votar_associadoIdEmBranco_retorna400() throws Exception {
        abrirSessao(5);
        String payload = """
                {"associadoId": "   ", "voto": "SIM"}
                """;

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("associadoId com 255 caracteres (limite) retorna 201")
    void votar_associadoIdCom255Caracteres_retorna201() throws Exception {
        abrirSessao(5);
        String associadoId = "a".repeat(255);
        String payload = "{\"associadoId\": \"" + associadoId + "\", \"voto\": \"SIM\"}";

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.associadoId").value(associadoId));
    }

    @Test
    @DisplayName("associadoId com 256 caracteres (acima do limite) retorna 400")
    void votar_associadoIdCom256Caracteres_retorna400() throws Exception {
        abrirSessao(5);
        String associadoId = "a".repeat(256);
        String payload = "{\"associadoId\": \"" + associadoId + "\", \"voto\": \"SIM\"}";

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        assertThat(votoRepository.countByPautaId(pautaId)).isZero();
    }

    @Test
    @DisplayName("associadoId com espaços e caixa diferente é tratado como o mesmo associado (normalização) e retorna 409")
    void votar_associadoIdComEspacosEMaiusculas_tratadoComoMesmoAssociado_retorna409() throws Exception {
        abrirSessao(5);
        String payloadOriginal = """
                {"associadoId": "associado-1", "voto": "SIM"}
                """;
        String payloadVariante = """
                {"associadoId": "Associado-1 ", "voto": "NAO"}
                """;

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadOriginal))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadVariante))
                .andExpect(status().isConflict());

        assertThat(votoRepository.countByPautaId(pautaId)).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-3.10: dois votos simultâneos do mesmo associado resultam em exatamente um 201 e um 409, com um único voto persistido")
    void votar_duasChamadasConcorrentesMesmoAssociado_exatamenteUmaSucede() throws Exception {
        abrirSessao(5);
        gancho.ativarBarreira();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> tarefa1 = executor.submit(this::votarComoAssociado1);
            Future<Integer> tarefa2 = executor.submit(this::votarComoAssociado1);

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
            assertThat(votoRepository.countByPautaId(pautaId)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private int votarComoAssociado1() throws Exception {
        String payload = """
                {"associadoId": "associado-concorrente", "voto": "SIM"}
                """;
        return mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andReturn().getResponse().getStatus();
    }
}

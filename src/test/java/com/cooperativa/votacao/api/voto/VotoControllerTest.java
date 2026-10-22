package com.cooperativa.votacao.api.voto;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cooperativa.votacao.application.pauta.PautaService;
import com.cooperativa.votacao.application.sessao.SessaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
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
class VotoControllerTest {

    private static final Instant T0 = Instant.parse("2026-10-22T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PautaService pautaService;

    @Autowired
    private SessaoService sessaoService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private Clock clock;

    private Long pautaId;

    @BeforeEach
    void setUp() {
        var pauta = pautaService.criar("Eleicao de diretoria " + System.nanoTime(), null);
        pautaId = pauta.getId();
    }

    @Test
    @DisplayName("AC-3.1: votos SIM e NAO de associados distintos em sessao aberta retornam 201")
    void votar_comSessaoAbertaEAssociadosDistintos_retorna201() throws Exception {
        when(clock.instant()).thenReturn(T0);
        sessaoService.abrir(pautaId, 5);

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associado": "associado-A", "voto": "SIM"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.pautaId").value(pautaId))
                .andExpect(jsonPath("$.voto").value("SIM"));

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associado": "associado-B", "voto": "NAO"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.pautaId").value(pautaId))
                .andExpect(jsonPath("$.voto").value("NAO"));
    }

    @Test
    @DisplayName("AC-3.2: valor de voto fora de SIM/NAO retorna 400")
    void votar_comValorDeVotoInvalido_retorna400() throws Exception {
        when(clock.instant()).thenReturn(T0);
        sessaoService.abrir(pautaId, 5);

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associado": "associado-A", "voto": "TALVEZ"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("AC-3.3: voto em pauta sem sessao aberta retorna 409")
    void votar_semSessaoAberta_retorna409() throws Exception {
        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associado": "associado-A", "voto": "SIM"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("AC-3.4: voto apos fim da sessao retorna 409")
    void votar_aposFimDaSessao_retorna409() throws Exception {
        when(clock.instant()).thenReturn(T0);
        sessaoService.abrir(pautaId, 1);
        // SessaoService.abrir chama clock.instant() exatamente 1 vez (abertura == agora);
        // verify explicito evita que a sequencia de stub abaixo desloque silenciosamente
        // se abrir() passar a chamar o clock mais de uma vez.
        verify(clock, times(1)).instant();

        when(clock.instant()).thenReturn(T0.plusSeconds(90));

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associado": "associado-A", "voto": "SIM"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("AC-3.5: segundo voto do mesmo associado na mesma pauta retorna 409")
    void votar_comAssociadoQueJaVotou_retorna409() throws Exception {
        when(clock.instant()).thenReturn(T0);
        sessaoService.abrir(pautaId, 5);

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associado": "associado-A", "voto": "SIM"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associado": "associado-A", "voto": "NAO"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("AC-3.6: GET resultado com sessao aberta retorna contagens e situacao aberta")
    void resultado_comSessaoAberta_retornaContagensESituacao() throws Exception {
        when(clock.instant()).thenReturn(T0);
        sessaoService.abrir(pautaId, 5);

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associado": "associado-A", "voto": "SIM"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associado": "associado-B", "voto": "SIM"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associado": "associado-C", "voto": "NAO"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/pautas/{id}/resultado", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sim").value(2))
                .andExpect(jsonPath("$.nao").value(1))
                .andExpect(jsonPath("$.situacao").value("aberta"));
    }

    @Test
    @DisplayName("AC-3.6: GET resultado com sessao encerrada retorna situacao encerrada")
    void resultado_comSessaoEncerrada_retornaSituacaoEncerrada() throws Exception {
        when(clock.instant()).thenReturn(T0).thenReturn(T0.plusSeconds(90));
        sessaoService.abrir(pautaId, 1);

        mockMvc.perform(get("/pautas/{id}/resultado", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("encerrada"));
    }

    @Test
    @DisplayName("AC-3.7: GET resultado para pauta inexistente retorna 404")
    void resultado_comPautaInexistente_retorna404() throws Exception {
        mockMvc.perform(get("/pautas/{id}/resultado", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("AC-3.8: POST voto para pauta inexistente retorna 404")
    void votar_comPautaInexistente_retorna404() throws Exception {
        mockMvc.perform(post("/pautas/{id}/votos", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associado": "associado-A", "voto": "SIM"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("AC-3.9: associado nulo, vazio ou acima de 50 caracteres retorna 400")
    void votar_comAssociadoInvalido_retorna400() throws Exception {
        when(clock.instant()).thenReturn(T0);
        sessaoService.abrir(pautaId, 5);

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"voto": "SIM"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associado": "", "voto": "SIM"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associado": "%s", "voto": "SIM"}
                                """.formatted("a".repeat(51))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /pautas/abc/votos (id nao numerico) retorna 400")
    void votar_comIdNaoNumerico_retorna400() throws Exception {
        mockMvc.perform(post("/pautas/{id}/votos", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associado": "associado-A", "voto": "SIM"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /pautas/abc/resultado (id nao numerico) retorna 400")
    void resultado_comIdNaoNumerico_retorna400() throws Exception {
        mockMvc.perform(get("/pautas/{id}/resultado", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /pautas/{id}/votos com Content-Type nao suportado retorna 415")
    void votar_comContentTypeNaoSuportado_retorna415() throws Exception {
        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("associado=associado-A"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    @DisplayName("POST /pautas/{id}/votos com body acima do limite retorna 413")
    void votar_comBodyMuitoGrande_retorna413() throws Exception {
        var payloadGigante = objectMapper.writeValueAsString(
                new VotoRequestComCampoExtra("associado-A", "SIM", "a".repeat(20_000)));

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadGigante))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(413));
    }

    private record VotoRequestComCampoExtra(String associado, String voto, String campoExtra) {
    }
}

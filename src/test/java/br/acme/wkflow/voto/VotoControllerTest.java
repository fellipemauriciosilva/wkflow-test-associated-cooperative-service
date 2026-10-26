package br.acme.wkflow.voto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.acme.wkflow.pauta.Pauta;
import br.acme.wkflow.pauta.PautaRepository;
import br.acme.wkflow.sessao.Sessao;
import br.acme.wkflow.sessao.SessaoRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class VotoControllerTest {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final Instant AGORA = Instant.parse("2026-10-26T12:00:00Z");
    private static final long PAUTA_INEXISTENTE = 999999L;

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

    @Autowired
    private VotoRepository votos;

    private Long pautaId;

    @BeforeEach
    void criarPautaComSessaoAberta() {
        pautaId = pautas.save(new Pauta("Aprovar contas")).getId();
        sessoes.save(new Sessao(pautaId, AGORA.minus(Duration.ofMinutes(1)), AGORA.plus(Duration.ofMinutes(5))));
    }

    private String votoJson(String associadoId, String valor) {
        return "{\"associadoId\": \"" + associadoId + "\", \"valor\": \"" + valor + "\"}";
    }

    @Test
    void ac31_votoComSessaoAbertaRetorna201() throws Exception {
        mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-1", "SIM")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pautaId").value(pautaId))
                .andExpect(jsonPath("$.associadoId").value("assoc-1"))
                .andExpect(jsonPath("$.valor").value("SIM"));
    }

    @Test
    void ac31_votoComSessaoNoFimExatoRetorna201() throws Exception {
        Long fronteira = pautas.save(new Pauta("Fronteira")).getId();
        sessoes.save(new Sessao(fronteira, AGORA.minus(Duration.ofMinutes(5)), AGORA));

        mvc.perform(post("/pautas/{id}/votos", fronteira)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-1", "SIM")))
                .andExpect(status().isCreated());
    }

    @Test
    void ac32_votoSemSessaoRetorna409() throws Exception {
        Long semSessao = pautas.save(new Pauta("Sem sessao")).getId();

        mvc.perform(post("/pautas/{id}/votos", semSessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-1", "SIM")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void ac32_votoComSessaoExpiradaRetorna409() throws Exception {
        Long expirada = pautas.save(new Pauta("Expirada")).getId();
        sessoes.save(new Sessao(expirada, AGORA.minus(Duration.ofMinutes(10)), AGORA.minus(Duration.ofMinutes(1))));

        mvc.perform(post("/pautas/{id}/votos", expirada)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-1", "SIM")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void ac33_votoEmPautaInexistenteRetorna404() throws Exception {
        mvc.perform(post("/pautas/{id}/votos", PAUTA_INEXISTENTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-1", "SIM")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void ac34_valorDiferenteDeSimOuNaoRetorna400() throws Exception {
        mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-1", "TALVEZ")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void ac34_valorNullRetorna400() throws Exception {
        mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\": \"assoc-1\", \"valor\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void ac35_segundoVotoMesmoAssociadoNaMesmaPautaRetorna409() throws Exception {
        mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-1", "SIM")))
                .andExpect(status().isCreated());

        mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-1", "NAO")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void ac35_constraintUnicaBloqueiaVotoDuplicadoNoBanco() {
        votos.saveAndFlush(new Voto(pautaId, "assoc-2", ValorVoto.SIM));

        assertThrows(DataIntegrityViolationException.class,
                () -> votos.saveAndFlush(new Voto(pautaId, "assoc-2", ValorVoto.NAO)));
    }

    @Test
    void ac35_violacaoDaConstraintDaTabelaVotoEhReconhecida() {
        votos.saveAndFlush(new Voto(pautaId, "assoc-4", ValorVoto.SIM));

        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,
                () -> votos.saveAndFlush(new Voto(pautaId, "assoc-4", ValorVoto.NAO)));

        assertTrue(VotoController.ehViolacaoDeUnicidade(ex));
    }

    @Test
    void ac35_outraViolacaoDeIntegridadeNaoEhTratadaComoDuplicidade() {
        var outra = new ConstraintViolationException("outra", null, "uk_outra");

        assertFalse(VotoController.ehViolacaoDeUnicidade(new DataIntegrityViolationException("outra", outra)));
    }

    @Test
    void ac35_violacaoSemConstraintNomeadaNaoEhTratadaComoDuplicidade() {
        assertFalse(VotoController.ehViolacaoDeUnicidade(new DataIntegrityViolationException("not null")));
    }

    @Test
    void ac35_associadoIdComEspacoExtraEhMesmoAssociado() throws Exception {
        mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-1", "SIM")))
                .andExpect(status().isCreated());

        mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-1 ", "NAO")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void ac35_detailDo409NaoExpoeAssociadoId() throws Exception {
        mvc.perform(post("/pautas/{id}/votos", pautaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(votoJson("assoc-segredo", "SIM")))
                .andExpect(status().isCreated());

        String corpo = mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-segredo", "SIM")))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        assertFalse(corpo.contains("assoc-segredo"));
    }

    @Test
    void ac36_resultadoMostraTotaisSimNaoETotal() throws Exception {
        mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-1", "SIM")))
                .andExpect(status().isCreated());
        mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-2", "SIM")))
                .andExpect(status().isCreated());
        mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-3", "NAO")))
                .andExpect(status().isCreated());

        mvc.perform(get("/pautas/{id}/resultado", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sim").value(2))
                .andExpect(jsonPath("$.nao").value(1))
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void ac37_resultadoDePautaInexistenteRetorna404() throws Exception {
        mvc.perform(get("/pautas/{id}/resultado", PAUTA_INEXISTENTE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void ac38_pautaInexistenteComValorInvalidoRetorna404() throws Exception {
        mvc.perform(post("/pautas/{id}/votos", PAUTA_INEXISTENTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-1", "TALVEZ")))
                .andExpect(status().isNotFound());
    }

    @Test
    void ac38_semSessaoComValorInvalidoRetorna409() throws Exception {
        Long semSessao = pautas.save(new Pauta("Sem sessao")).getId();

        mvc.perform(post("/pautas/{id}/votos", semSessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-1", "TALVEZ")))
                .andExpect(status().isConflict());
    }

    @Test
    void ac38_duplicadoComValorInvalidoRetorna400() throws Exception {
        mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-1", "SIM")))
                .andExpect(status().isCreated());

        mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-1", "TALVEZ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void associadoIdEmBrancoRetorna400() throws Exception {
        mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("   ", "SIM")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void associadoIdNullRetorna400() throws Exception {
        mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\": null, \"valor\": \"SIM\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void associadoIdComCaractereInvisivelRetorna400SemCriarVoto() throws Exception {
        String[] invisiveis = {
                "assoc-1\\u00A0", "assoc\\u200B-1", "\\uFEFFassoc-1", "assoc-1\\u2028", "assoc\\u0000-1"
        };
        for (String associado : invisiveis) {
            Long pauta = pautas.save(new Pauta("Invisivel")).getId();
            sessoes.save(new Sessao(pauta, AGORA.minus(Duration.ofMinutes(1)), AGORA.plus(Duration.ofMinutes(5))));

            mvc.perform(post("/pautas/{id}/votos", pauta)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(votoJson(associado, "SIM")))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));

            assertEquals(0L, votos.findAll().stream().filter(v -> pauta.equals(v.getPautaId())).count());
        }
    }

    @Test
    void associadoIdComNbspDepoisDeVotoExistenteRetorna400SemVotoNovo() throws Exception {
        mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-1", "SIM")))
                .andExpect(status().isCreated());

        mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("assoc-1\\u00A0", "NAO")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));

        assertEquals(1L, votos.findAll().stream().filter(v -> pautaId.equals(v.getPautaId())).count());
    }

    @Test
    void associadoIdAcimaDe255CaracteresRetorna400() throws Exception {
        mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(votoJson("a".repeat(256), "SIM")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void resultadoSemVotosRetornaZeros() throws Exception {
        mvc.perform(get("/pautas/{id}/resultado", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sim").value(0))
                .andExpect(jsonPath("$.nao").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }
}

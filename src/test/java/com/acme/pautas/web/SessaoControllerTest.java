package com.acme.pautas.web;

import com.acme.pautas.RelogioDeTeste;
import com.acme.pautas.repository.SessaoRepository;
import com.acme.pautas.service.PautaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RelogioDeTeste.class)
class SessaoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PautaService pautas;

    @Autowired
    private SessaoRepository sessoes;

    @Autowired
    private RelogioDeTeste.RelogioControlavel relogio;

    @BeforeEach
    void relogioNoInicio() {
        relogio.ajustarPara(RelogioDeTeste.INICIO);
    }

    @Test
    void abreSessaoComDuracaoInformada() throws Exception {
        Long pautaId = pautas.cadastrar("Pauta com duração", null).getId();

        mvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\": 5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.pautaId").value(pautaId))
                .andExpect(jsonPath("$.abertura").value("2026-10-24T12:00:00Z"))
                .andExpect(jsonPath("$.fim").value("2026-10-24T12:05:00Z"));
    }

    @Test
    void usaDuracaoPadraoDeUmMinutoSemBody() throws Exception {
        Long pautaId = pautas.cadastrar("Pauta sem body", null).getId();

        mvc.perform(post("/pautas/{id}/sessao", pautaId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.abertura").value("2026-10-24T12:00:00Z"))
                .andExpect(jsonPath("$.fim").value("2026-10-24T12:01:00Z"));
    }

    @Test
    void pautaInexistenteRetorna404NoFormatoPadrao() throws Exception {
        mvc.perform(post("/pautas/{id}/sessao", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.path").value("/pautas/999999/sessao"));
    }

    @Test
    void segundaAberturaComSessaoAtivaRetorna409ePreservaSessaoOriginal() throws Exception {
        Long pautaId = pautas.cadastrar("Pauta com sessão ativa", null).getId();
        mvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\": 5}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\": 30}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/pautas/" + pautaId + "/sessao"));

        assertEquals(1, sessoes.countByPautaId(pautaId));
        assertEquals(Instant.parse("2026-10-24T12:05:00Z"),
                sessoes.findAll().stream()
                        .filter(s -> s.getPautaId().equals(pautaId))
                        .findFirst().orElseThrow().getFim());
    }

    @Test
    void sessaoExpiradaNaoBloqueiaNovaAbertura() throws Exception {
        Long pautaId = pautas.cadastrar("Pauta com sessão expirada", null).getId();
        mvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\": 1}"))
                .andExpect(status().isCreated());

        relogio.ajustarPara(Instant.parse("2026-10-24T12:01:00Z"));

        mvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\": 2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.abertura").value("2026-10-24T12:01:00Z"))
                .andExpect(jsonPath("$.fim").value("2026-10-24T12:03:00Z"));
    }

    @Test
    void duracaoInvalidaRetorna400enaoGravaSessao() throws Exception {
        Long pautaId = pautas.cadastrar("Pauta com duração inválida", null).getId();

        for (String valor : new String[]{"0", "-1", "\"abc\""}) {
            mvc.perform(post("/pautas/{id}/sessao", pautaId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"duracaoMinutos\": " + valor + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"));
        }

        assertEquals(0, sessoes.countByPautaId(pautaId));
    }
}

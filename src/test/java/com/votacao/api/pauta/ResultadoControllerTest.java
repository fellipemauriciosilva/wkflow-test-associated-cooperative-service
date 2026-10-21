package com.votacao.api.pauta;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResultadoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long criarPauta(String titulo) throws Exception {
        String body = objectMapper.writeValueAsString(new PautaRequestFixture(titulo, "desc"));
        String response = mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void abrirSessao(Long pautaId, int duracaoMinutos) throws Exception {
        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracao_minutos\":" + duracaoMinutos + "}"))
                .andExpect(status().isCreated());
    }

    private void votar(Long pautaId, long idAssociado, String voto) throws Exception {
        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_associado\":" + idAssociado + ",\"voto\":\"" + voto + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void apurarResultado_pautaSemSessao_retorna200ComNaoIniciadaEZerado() throws Exception {
        Long pautaId = criarPauta("Pauta Resultado A");

        mockMvc.perform(get("/pautas/{id}/resultado", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status_sessao").value("NAO_INICIADA"))
                .andExpect(jsonPath("$.votos_sim").value(0))
                .andExpect(jsonPath("$.votos_nao").value(0))
                .andExpect(jsonPath("$.total_votos").value(0))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void apurarResultado_sessaoAbertaComVotos_retorna200ComContagensEStatusAberta() throws Exception {
        Long pautaId = criarPauta("Pauta Resultado B");
        abrirSessao(pautaId, 30);
        votar(pautaId, 1L, "SIM");
        votar(pautaId, 2L, "SIM");
        votar(pautaId, 3L, "NAO");

        mockMvc.perform(get("/pautas/{id}/resultado", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status_sessao").value("ABERTA"))
                .andExpect(jsonPath("$.votos_sim").value(2))
                .andExpect(jsonPath("$.votos_nao").value(1))
                .andExpect(jsonPath("$.total_votos").value(3));
    }

    @Test
    void apurarResultado_pautaInexistente_retorna404() throws Exception {
        mockMvc.perform(get("/pautas/{id}/resultado", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.path").value("/pautas/999999/resultado"));
    }

    private record PautaRequestFixture(String titulo, String descricao) {
    }
}

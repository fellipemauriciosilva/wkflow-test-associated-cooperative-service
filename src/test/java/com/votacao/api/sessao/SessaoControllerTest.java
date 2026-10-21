package com.votacao.api.sessao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SessaoControllerTest {

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

    @Test
    void abrirSessao_comDuracaoCustom_retorna201ComFechamentoCalculado() throws Exception {
        Long pautaId = criarPauta("Pauta Sessao A");

        String response = mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracao_minutos\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.abertura").exists())
                .andExpect(jsonPath("$.fechamento").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var json = objectMapper.readTree(response);
        LocalDateTime abertura = LocalDateTime.parse(json.get("abertura").asText());
        LocalDateTime fechamento = LocalDateTime.parse(json.get("fechamento").asText());
        org.assertj.core.api.Assertions.assertThat(Duration.between(abertura, fechamento).toMinutes()).isEqualTo(5);
    }

    @Test
    void abrirSessao_semCorpo_usaDuracaoPadraoDeUmMinuto() throws Exception {
        Long pautaId = criarPauta("Pauta Sessao B");

        String response = mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var json = objectMapper.readTree(response);
        LocalDateTime abertura = LocalDateTime.parse(json.get("abertura").asText());
        LocalDateTime fechamento = LocalDateTime.parse(json.get("fechamento").asText());
        org.assertj.core.api.Assertions.assertThat(Duration.between(abertura, fechamento).toMinutes()).isEqualTo(1);
    }

    @Test
    void abrirSessao_comCorpoVazio_usaDuracaoPadraoDeUmMinuto() throws Exception {
        Long pautaId = criarPauta("Pauta Sessao C");

        String response = mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var json = objectMapper.readTree(response);
        LocalDateTime abertura = LocalDateTime.parse(json.get("abertura").asText());
        LocalDateTime fechamento = LocalDateTime.parse(json.get("fechamento").asText());
        org.assertj.core.api.Assertions.assertThat(Duration.between(abertura, fechamento).toMinutes()).isEqualTo(1);
    }

    @Test
    void abrirSessao_comDuracaoZero_retorna400() throws Exception {
        Long pautaId = criarPauta("Pauta Sessao D");

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracao_minutos\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void abrirSessao_comDuracaoNegativa_retorna400() throws Exception {
        Long pautaId = criarPauta("Pauta Sessao E");

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracao_minutos\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void abrirSessao_segundaVezNaMesmaPauta_retorna422() throws Exception {
        Long pautaId = criarPauta("Pauta Sessao F");

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void abrirSessao_pautaInexistente_retorna404() throws Exception {
        mockMvc.perform(post("/pautas/{id}/sessao", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    private record PautaRequestFixture(String titulo, String descricao) {
    }
}

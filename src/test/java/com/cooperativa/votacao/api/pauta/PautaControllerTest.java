package com.cooperativa.votacao.api.pauta;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PautaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /pautas com dados validos retorna 201 com id")
    void criar_comDadosValidos_retorna201ComId() throws Exception {
        var payload = """
                {"titulo": "Eleicao de diretoria", "descricao": "Votacao anual"}
                """;

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.titulo").value("Eleicao de diretoria"))
                .andExpect(jsonPath("$.descricao").value("Votacao anual"));
    }

    @Test
    @DisplayName("POST /pautas sem titulo retorna 400")
    void criar_semTitulo_retorna400() throws Exception {
        var payload = """
                {"descricao": "Votacao anual"}
                """;

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem", notNullValue()));
    }

    @Test
    @DisplayName("POST /pautas com titulo em branco retorna 400")
    void criar_comTituloEmBranco_retorna400() throws Exception {
        var payload = """
                {"titulo": "   ", "descricao": "Votacao anual"}
                """;

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /pautas/{id} com pauta existente retorna 200")
    void buscarPorId_comPautaExistente_retorna200() throws Exception {
        var payload = """
                {"titulo": "Eleicao de diretoria", "descricao": "Votacao anual"}
                """;

        var resultadoCriacao = mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        var idCriado = objectMapper.readTree(resultadoCriacao.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/pautas/{id}", idCriado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idCriado))
                .andExpect(jsonPath("$.titulo").value("Eleicao de diretoria"));
    }

    @Test
    @DisplayName("GET /pautas/{id} com pauta inexistente retorna 404")
    void buscarPorId_comPautaInexistente_retorna404() throws Exception {
        mockMvc.perform(get("/pautas/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").value("Pauta nao encontrada"));
    }
}

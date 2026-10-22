package com.acme.votacao.pauta;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PautaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /pautas com titulo válido retorna 201 com id")
    void criar_payloadValido_retorna201ComId() throws Exception {
        String payload = """
                {"titulo": "Reforma do estatuto", "descricao": "Discussão sobre regras internas"}
                """;

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @DisplayName("GET /pautas/{id} com id existente retorna 200 com dados")
    void buscarPorId_idExistente_retorna200() throws Exception {
        String payload = """
                {"titulo": "Eleição diretoria", "descricao": "Votação anual"}
                """;

        String responseBody = mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(responseBody).get("id").asLong();

        mockMvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Eleição diretoria"))
                .andExpect(jsonPath("$.descricao").value("Votação anual"));
    }

    @Test
    @DisplayName("GET /pautas/{id} com id inexistente retorna 404")
    void buscarPorId_idInexistente_retorna404() throws Exception {
        mockMvc.perform(get("/pautas/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /pautas sem titulo retorna 400")
    void criar_semTitulo_retorna400() throws Exception {
        String payload = """
                {"descricao": "Sem titulo"}
                """;

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /pautas com titulo nulo retorna 400")
    void criar_tituloNulo_retorna400() throws Exception {
        String payload = """
                {"titulo": null, "descricao": "Sem titulo"}
                """;

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /pautas com titulo em branco retorna 400")
    void criar_tituloEmBranco_retorna400() throws Exception {
        String payload = """
                {"titulo": "   ", "descricao": "Sem titulo"}
                """;

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }
}

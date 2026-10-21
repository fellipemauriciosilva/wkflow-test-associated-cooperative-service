package com.votacao.api.pauta;

import static org.hamcrest.Matchers.greaterThan;
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
class PautaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void criarPauta_comDadosValidos_retorna201ComIdECriacao() throws Exception {
        String body = objectMapper.writeValueAsString(new PautaRequestFixture("Pauta A", "Descrição A"));

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(greaterThan(0)))
                .andExpect(jsonPath("$.titulo").value("Pauta A"))
                .andExpect(jsonPath("$.criacao").exists());
    }

    @Test
    void criarPauta_comTituloVazio_retorna400NoFormatoPadrao() throws Exception {
        String body = objectMapper.writeValueAsString(new PautaRequestFixture("", "x"));

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/pautas"));
    }

    @Test
    void criarPauta_comTituloExcedendoTamanhoMaximo_retorna400NoFormatoPadrao() throws Exception {
        String tituloGrande = "a".repeat(256);
        String body = objectMapper.writeValueAsString(new PautaRequestFixture(tituloGrande, "x"));

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/pautas"));
    }

    @Test
    void criarPauta_comDescricaoExcedendoTamanhoMaximo_retorna400NoFormatoPadrao() throws Exception {
        String descricaoGrande = "a".repeat(256);
        String body = objectMapper.writeValueAsString(new PautaRequestFixture("Pauta", descricaoGrande));

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void criarPauta_comTituloAusente_retorna400() throws Exception {
        String body = "{\"descricao\":\"x\"}";

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void buscarPauta_existente_retorna200ComPautaCompleta() throws Exception {
        String body = objectMapper.writeValueAsString(new PautaRequestFixture("Pauta B", "Descrição B"));

        String response = mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Pauta B"))
                .andExpect(jsonPath("$.descricao").value("Descrição B"))
                .andExpect(jsonPath("$.criacao").exists());
    }

    @Test
    void buscarPauta_inexistente_retorna404NoFormatoPadrao() throws Exception {
        mockMvc.perform(get("/pautas/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/pautas/999999"));
    }

    private record PautaRequestFixture(String titulo, String descricao) {
    }
}

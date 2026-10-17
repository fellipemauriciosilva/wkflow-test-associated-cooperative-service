package com.cooperative.voting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link PautaController} using a real, in-memory H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PautaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreatePauta_Success() throws Exception {
        Map<String, Object> body = Map.of(
                "title", "Pauta 1",
                "description", "Descricao da pauta",
                "date", "2026-02-15T10:00:00"
        );

        mockMvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/pautas/\\d+")))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Pauta 1"))
                .andExpect(jsonPath("$.description").value("Descricao da pauta"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void testCreatePauta_ValidationFailure_BlankTitle() throws Exception {
        Map<String, Object> body = Map.of(
                "title", "",
                "description", "Descricao da pauta",
                "date", "2026-02-15T10:00:00"
        );

        mockMvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]").value("title: must not be blank"));
    }

    @Test
    void testCreatePauta_ValidationFailure_NullDate() throws Exception {
        Map<String, Object> body = Map.of(
                "title", "Pauta 1",
                "description", "Descricao da pauta"
        );

        mockMvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));
    }

    @Test
    void testGetPautaById_Success() throws Exception {
        Map<String, Object> body = Map.of(
                "title", "Pauta Consulta",
                "description", "Descricao consulta",
                "date", "2026-03-01T09:00:00"
        );

        String response = mockMvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/pautas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Pauta Consulta"));

        mockMvc.perform(get("/pautas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void testGetPautaById_NotFound() throws Exception {
        mockMvc.perform(get("/pautas/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Pauta with id 99999 not found"));
    }
}

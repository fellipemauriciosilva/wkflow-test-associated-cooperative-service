package com.example.pauta.controller;

import com.example.pauta.repository.PautaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PautaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PautaRepository pautaRepository;

    @Test
    void createPautaWithValidTitle_returns201WithLocationAndBody() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("title", "Eleição Conselho 2024"));

        mockMvc.perform(post("/pautas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/pautas/\\d+")))
                .andExpect(jsonPath("$.id", greaterThan(0)))
                .andExpect(jsonPath("$.title").value("Eleição Conselho 2024"))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    void createPauta_rejectsNullBlankOrMissingTitle() throws Exception {
        mockMvc.perform(post("/pautas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("title")));

        mockMvc.perform(post("/pautas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("title")));

        mockMvc.perform(post("/pautas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("title")));

        mockMvc.perform(post("/pautas").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("title")));

        assertThat(pautaRepository.count()).isZero();
    }

    @Test
    void getExistingPauta_returns200WithMatchingData() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("title", "Reunião Trimestral"));

        MvcResult createResult = mockMvc.perform(post("/pautas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> created = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        Object id = created.get("id");

        mockMvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(((Number) id).longValue()))
                .andExpect(jsonPath("$.title").value("Reunião Trimestral"))
                .andExpect(jsonPath("$.createdAt").value(created.get("createdAt")));
    }

    @Test
    void getNonExistentPauta_returns404() throws Exception {
        mockMvc.perform(get("/pautas/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void createdPauta_persistsInRepository() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("title", "Persistência Direta"));

        MvcResult result = mockMvc.perform(post("/pautas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> created = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Long id = ((Number) created.get("id")).longValue();

        assertThat(pautaRepository.findById(id)).isPresent();
        assertThat(pautaRepository.findById(id).get().getTitle()).isEqualTo("Persistência Direta");
    }

    @Test
    void createdAt_isAutoPopulatedWithinFiveSeconds() throws Exception {
        Instant before = Instant.now();
        String body = objectMapper.writeValueAsString(Map.of("title", "Timestamp Automático"));

        MvcResult result = mockMvc.perform(post("/pautas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> created = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Instant createdAt = Instant.parse((String) created.get("createdAt"));

        assertThat(Math.abs(createdAt.toEpochMilli() - before.toEpochMilli())).isLessThan(5000);
    }

    @Test
    void otherHttpMethods_areRejected() throws Exception {
        mockMvc.perform(put("/pautas/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/pautas/1"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(patch("/pautas/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }
}

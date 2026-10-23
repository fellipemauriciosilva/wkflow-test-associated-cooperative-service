package com.acme.pautas.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PautaControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void criaPautaComTituloEDescricaoRetorna201ComIdELocation() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content("{\"titulo\":\"Aprovar balanço\",\"descricao\":\"Balanço 2025\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/pautas/")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.titulo").value("Aprovar balanço"))
                .andExpect(jsonPath("$.descricao").value("Balanço 2025"));
    }

    @Test
    void criaPautaSemDescricaoRetorna201() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content("{\"titulo\":\"Só título\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.descricao").doesNotExist());
    }

    @Test
    void criaPautaSemTituloRetorna400() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content("{\"descricao\":\"Sem título\"}"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void criaPautaComTituloVazioOuEspacosRetorna400(String titulo) throws Exception {
        mvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content("{\"titulo\":\"" + titulo + "\",\"descricao\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buscaPautaExistenteRetorna200ComTituloEDescricao() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content("{\"titulo\":\"Eleger diretoria\",\"descricao\":\"Mandato 2027\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id = criada.getResponse().getContentAsString()
                .replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Long.parseLong(id)))
                .andExpect(jsonPath("$.titulo").value("Eleger diretoria"))
                .andExpect(jsonPath("$.descricao").value("Mandato 2027"));
    }

    @Test
    void buscaPautaInexistenteRetorna404() throws Exception {
        mvc.perform(get("/pautas/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }
}

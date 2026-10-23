package br.coop.acme.pauta.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PautaControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void postComTituloValidoRetorna201ComId() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Aprovar contas\",\"descricao\":\"Contas do exercicio\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void postSemDescricaoRetorna201() throws Exception {
        String corpoCriado = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Sem descricao\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = JsonPath.parse(corpoCriado).read("$.id", Long.class);

        mvc.perform(get("/pautas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Sem descricao"))
                .andExpect(jsonPath("$.descricao").value(nullValue()));
    }

    @Test
    void postComDescricaoNulaRetorna201() throws Exception {
        String corpoCriado = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Descricao nula\",\"descricao\":null}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = JsonPath.parse(corpoCriado).read("$.id", Long.class);

        mvc.perform(get("/pautas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Descricao nula"))
                .andExpect(jsonPath("$.descricao").value(nullValue()));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void postComTituloInvalidoRetorna400(String titulo) throws Exception {
        String corpo = titulo == null
                ? "{\"descricao\":\"qualquer\"}"
                : "{\"titulo\":\"" + titulo + "\",\"descricao\":\"qualquer\"}";

        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getComIdExistenteRetorna200ComDados() throws Exception {
        String corpoCriado = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Pauta consulta\",\"descricao\":\"Detalhe\"}"))
                .andReturn().getResponse().getContentAsString();
        long id = JsonPath.parse(corpoCriado).read("$.id", Long.class);

        mvc.perform(get("/pautas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Pauta consulta"))
                .andExpect(jsonPath("$.descricao").value("Detalhe"));
    }

    @Test
    void getComIdInexistenteRetorna404() throws Exception {
        mvc.perform(get("/pautas/999999999"))
                .andExpect(status().isNotFound());
    }
}

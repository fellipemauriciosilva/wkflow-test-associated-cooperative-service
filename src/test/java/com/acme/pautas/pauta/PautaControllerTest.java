package com.acme.pautas.pauta;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PautaControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void postComTituloValidoRetorna201ComId() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Pauta X\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()));
    }

    @Test
    void postRetornaHeaderLocationComIdCriado() throws Exception {
        MvcResult result = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Pauta com location\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", notNullValue()))
                .andReturn();

        Long id = Long.valueOf(result.getResponse().getContentAsString()
                .replaceAll("\\D", ""));
        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk());
        String location = result.getResponse().getHeader("Location");
        org.assertj.core.api.Assertions.assertThat(location).isEqualTo("/pautas/" + id);
    }

    @Test
    void postSemTituloRetorna400() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value(notNullValue()))
                .andExpect(jsonPath("$.detalhes[0]").value("titulo: obrigatório e não pode ser vazio"));
    }

    @Test
    void postJsonMalformadoRetorna400ComCorpoJson() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{titulo: "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Requisição malformada"));
    }

    @Test
    void getIdNaoNumericoRetorna400ComCorpoJson() throws Exception {
        mvc.perform(get("/pautas/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Requisição malformada"));
    }

    @Test
    void postComTituloVazioRetorna400() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void postComTituloSoEspacosRetorna400() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void postComTituloAcimaDoLimiteRetorna400() throws Exception {
        String titulo = "a".repeat(256);
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"" + titulo + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes[0]").value("titulo: máximo de 255 caracteres"));
    }

    @Test
    void postComTituloNoLimiteRetorna201() throws Exception {
        String titulo = "a".repeat(255);
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"" + titulo + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void postComDescricaoAcimaDoLimiteRetorna400() throws Exception {
        String descricao = "d".repeat(2001);
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Pauta\", \"descricao\": \"" + descricao + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes[0]").value("descricao: máximo de 2000 caracteres"));
    }

    @Test
    void postComDescricaoNoLimiteRetorna201() throws Exception {
        String descricao = "d".repeat(2000);
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Pauta\", \"descricao\": \"" + descricao + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void postComCorpoAcimaDoLimiteRetorna413ComCorpoJson() throws Exception {
        String descricao = "d".repeat(20000);
        String corpo = "{\"titulo\": \"Pauta\", \"descricao\": \"" + descricao + "\"}";
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Content-Length", corpo.length())
                        .content(corpo))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.erro").value("Requisição muito grande"));
    }

    @Test
    void postComDescricaoAceitaRetorna201() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Pauta com descricao\", \"descricao\": \"Detalhes\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(greaterThan(0)));
    }

    @Test
    void postSemDescricaoAceitaRetorna201() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Pauta sem descricao\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(greaterThan(0)));
    }

    @Test
    void getPautaExistenteRetorna200ComCampos() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Pauta Y\", \"descricao\": \"Desc Y\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String idJson = criada.getResponse().getContentAsString();
        Long id = Long.valueOf(idJson.replaceAll("\\D", ""));

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Pauta Y"))
                .andExpect(jsonPath("$.descricao").value("Desc Y"));
    }

    @Test
    void getPautaSemDescricaoRetornaDescricaoNula() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Pauta Z\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = Long.valueOf(criada.getResponse().getContentAsString().replaceAll("\\D", ""));

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Pauta Z"))
                .andExpect(jsonPath("$.descricao").value(nullValue()));
    }

    @Test
    void getPautaInexistenteRetorna404ComCorpoJson() throws Exception {
        mvc.perform(get("/pautas/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value(notNullValue()));
    }
}

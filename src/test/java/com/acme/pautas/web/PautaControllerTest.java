package com.acme.pautas.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
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
    void cadastraPautaComTituloERetornaId() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Aprovar contas\", \"descricao\": \"Contas do exercício\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.titulo").value("Aprovar contas"))
                .andExpect(jsonPath("$.descricao").value("Contas do exercício"));
    }

    @Test
    void consultaPautaExistente() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Eleger diretoria\", \"descricao\": \"Chapa única\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id = criada.getResponse().getContentAsString()
                .replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Long.parseLong(id)))
                .andExpect(jsonPath("$.titulo").value("Eleger diretoria"))
                .andExpect(jsonPath("$.descricao").value("Chapa única"));
    }

    @Test
    void consultaPautaInexistenteRetorna404NoFormatoPadrao() throws Exception {
        mvc.perform(get("/pautas/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensagem").value("Pauta 999999 não encontrada."))
                .andExpect(jsonPath("$.path").value("/pautas/999999"));
    }

    @Test
    void cadastroSemTituloRetorna400() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\": \"sem titulo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").value(containsString("titulo")))
                .andExpect(jsonPath("$.path").value("/pautas"));
    }

    @Test
    void cadastroComTituloEmBrancoRetorna400() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").value(containsString("titulo")));
    }

    @Test
    void erros400e404TemMesmasChavesNoCorpo() throws Exception {
        MvcResult erro400 = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        MvcResult erro404 = mvc.perform(get("/pautas/{id}", 999998L))
                .andExpect(status().isNotFound())
                .andReturn();

        String corpo400 = erro400.getResponse().getContentAsString();
        String corpo404 = erro404.getResponse().getContentAsString();
        for (String chave : new String[] {"\"status\"", "\"erro\"", "\"mensagem\"", "\"path\""}) {
            org.junit.jupiter.api.Assertions.assertTrue(corpo400.contains(chave), "400 sem " + chave);
            org.junit.jupiter.api.Assertions.assertTrue(corpo404.contains(chave), "404 sem " + chave);
        }
    }
}

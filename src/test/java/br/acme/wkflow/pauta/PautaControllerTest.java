package br.acme.wkflow.pauta;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    private static final String PROBLEM_JSON = "application/problem+json";

    @Autowired
    private MockMvc mvc;

    @Test
    void ac11_postCriaPautaERetornaId() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\": \"Aprovar contas\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/pautas/")))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void ac12_getRetornaPautaExistente() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\": \"Eleger diretoria\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(criada.getResponse().getContentAsString(), "$.id").toString();

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Long.parseLong(id)))
                .andExpect(jsonPath("$.descricao").value("Eleger diretoria"));
    }

    @Test
    void ac13_getComIdInexistenteRetorna404ProblemDetail() throws Exception {
        mvc.perform(get("/pautas/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value(notNullValue()))
                .andExpect(jsonPath("$.detail").value(containsString("999999")));
    }

    @Test
    void ac14_postComDescricaoNulaVaziaOuEspacosRetorna400() throws Exception {
        String[] corpos = {
                "{\"descricao\": null}",
                "{\"descricao\": \"\"}",
                "{\"descricao\": \"   \"}",
                "{}"
        };
        for (String corpo : corpos) {
            mvc.perform(post("/pautas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void ac14_postSemCorpoRetorna400() throws Exception {
        mvc.perform(post("/pautas").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void ac15_erro400UsaProblemDetail() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value(notNullValue()))
                .andExpect(jsonPath("$.detail").value(notNullValue()));
    }
}

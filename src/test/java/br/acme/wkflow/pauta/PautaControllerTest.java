package br.acme.wkflow.pauta;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
        Long id = ((Number) JsonPath.read(criada.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.descricao").value("Eleger diretoria"));
    }

    @Test
    void ac13_getComIdInexistenteRetorna404ProblemDetail() throws Exception {
        mvc.perform(get("/pautas/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
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
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
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
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Requisicao invalida"));
    }

    @Test
    void getComIdNaoNumericoRetorna400ProblemDetail() throws Exception {
        mvc.perform(get("/pautas/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void postComContentTypeTextoRetorna415ProblemDetail() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("descricao"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void postComDescricaoDe256CaracteresRetorna400SemErro500() throws Exception {
        String descricao = "a".repeat(256);
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\": \"" + descricao + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }
}

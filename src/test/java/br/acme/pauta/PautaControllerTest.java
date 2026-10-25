package br.acme.pauta;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
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

    private static final String CORPO_VALIDO = """
            {"titulo": "Aprovar contas", "descricao": "Contas do exercicio"}
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    void criarPautaValida_retorna201ComId() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/pautas/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.titulo").value("Aprovar contas"))
                .andExpect(jsonPath("$.descricao").value("Contas do exercicio"));
    }

    @Test
    void consultarPautaExistente_retorna200ComDados() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "Consulta", "descricao": "Detalhe"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(criada.getResponse().getContentAsString(), "$.id").toString();

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Consulta"))
                .andExpect(jsonPath("$.descricao").value("Detalhe"));
    }

    @Test
    void consultarPautaInexistente_retorna404FormatoPadrao() throws Exception {
        mvc.perform(get("/pautas/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(not(emptyString())))
                .andExpect(jsonPath("$.path").value("/pautas/999999"));
    }

    @Test
    void criarPautaSemTitulo_retorna400FormatoPadrao() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao": "Sem titulo"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/pautas"))
                .andExpect(jsonPath("$.message").value(containsString("titulo")));
    }

    @Test
    void criarPautaComTituloEmBranco_retorna400CitandoTitulo() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "   ", "descricao": "Espacos"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("titulo")));
    }
}

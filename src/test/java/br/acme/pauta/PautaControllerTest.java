package br.acme.pauta;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PautaControllerTest.ErroInternoController.class)
class PautaControllerTest {

    @RestController
    static class ErroInternoController {
        @GetMapping("/teste/erro-interno")
        void lanca() {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "segredo");
        }
    }

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
                .andExpect(header().string("Location", startsWith("/pautas/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.titulo").value("Aprovar contas"))
                .andExpect(jsonPath("$.descricao").value("Contas do exercicio"));
    }

    @Test
    void criarPautaSemDescricao_retorna201() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "Sem descricao"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/pautas/")))
                .andExpect(jsonPath("$.titulo").value("Sem descricao"))
                .andExpect(jsonPath("$.descricao").doesNotExist());
    }

    @Test
    void criarPautaComIdDoCliente_ignoraIdEnviado() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": 424242, "titulo": "Id ignorado"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        if (JsonPath.<Number>read(criada.getResponse().getContentAsString(), "$.id").longValue() == 424242L) {
            throw new AssertionError("id enviado pelo cliente foi usado");
        }
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
        String id = JsonPath.read(criada.getResponse().getContentAsString(), "$.id").toString();

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Consulta"))
                .andExpect(jsonPath("$.descricao").value("Detalhe"));
    }

    @Test
    void consultarPautaComTituloEDescricaoComEspacos_retornaValoresAparados() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "  Aprovar  ", "descricao": "  Contas  "}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String id = JsonPath.read(criada.getResponse().getContentAsString(), "$.id").toString();

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Aprovar"))
                .andExpect(jsonPath("$.descricao").value("Contas"));
    }

    @Test
    void consultarPautaInexistente_retorna404FormatoPadrao() throws Exception {
        String id = String.valueOf(Long.MAX_VALUE);

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(containsString(id)))
                .andExpect(jsonPath("$.path").value("/pautas/" + id));
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
                .andExpect(jsonPath("$.message").value("titulo: obrigatorio"));
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
                .andExpect(jsonPath("$.message").value("titulo: obrigatorio"));
    }

    @Test
    void criarPautaComTituloNoLimite_retorna201() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("a".repeat(Pauta.TAMANHO_MAXIMO), "ok")))
                .andExpect(status().isCreated());
    }

    @Test
    void criarPautaComTituloAcimaDoLimite_retorna400() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("a".repeat(Pauta.TAMANHO_MAXIMO + 1), "ok")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("titulo")));
    }

    @Test
    void criarPautaComDescricaoAcimaDoLimite_retorna400() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Titulo", "d".repeat(Pauta.TAMANHO_MAXIMO + 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("descricao")));
    }

    @Test
    void criarPautaComTituloNoLimiteComEspacosNasBordas_retorna201ComTamanhoAparado() throws Exception {
        String titulo = "a".repeat(Pauta.TAMANHO_MAXIMO);
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("  " + titulo + "  ", "ok")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value(titulo));
    }

    @Test
    void criarPautaComDescricaoSoEspacos_persisteSemDescricao() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Titulo", "   ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").doesNotExist());
    }

    @Test
    void corpoJsonMalformado_retorna400FormatoPadrao() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("corpo: JSON invalido"))
                .andExpect(jsonPath("$.path").value("/pautas"));
    }

    @Test
    void idNaoNumerico_retorna400FormatoPadrao() throws Exception {
        mvc.perform(get("/pautas/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("id: formato invalido"))
                .andExpect(jsonPath("$.path").value("/pautas/abc"));
    }

    @Test
    void rotaInexistente_retorna404MantendoStatus() throws Exception {
        mvc.perform(get("/rota-que-nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/rota-que-nao-existe"));
    }

    @Test
    void metodoNaoSuportado_retorna405MantendoStatus() throws Exception {
        mvc.perform(delete("/pautas/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.path").value("/pautas/1"));
    }

    @Test
    void tipoDeMidiaNaoSuportado_retorna415MantendoStatus() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("titulo=x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.message").value(not(emptyString())));
    }

    @Test
    void erroInterno_retorna500SemVazarMensagem() throws Exception {
        mvc.perform(get("/teste/erro-interno"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Erro interno do servidor"))
                .andExpect(content().string(not(containsString("segredo"))));
    }

    private static String corpo(String titulo, String descricao) {
        return "{\"titulo\": \"" + titulo + "\", \"descricao\": \"" + descricao + "\"}";
    }
}

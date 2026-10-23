package pautas;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Stream;

@SpringBootTest
@AutoConfigureMockMvc
class PautaControllerTest {

    private static final String TITULO = "Aprovação do balanço 2024";
    private static final String DESCRICAO = "Opcional";

    @Autowired
    private MockMvc mvc;

    @Test
    void cadastraPautaValidaRetorna201ComLocationEId() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(TITULO, DESCRICAO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesRegex("/pautas/\\d+")))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void buscaPautaCadastradaRetornaMesmosCampos() throws Exception {
        long id = cadastrar(TITULO, DESCRICAO);

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value(TITULO))
                .andExpect(jsonPath("$.descricao").value(DESCRICAO));
    }

    @Test
    void buscaPautaSemDescricaoRetornaDescricaoNula() throws Exception {
        long id = cadastrar(TITULO, null);

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value(nullValue()));
    }

    @Test
    void buscaIdInexistenteRetorna404Padronizado() throws Exception {
        mvc.perform(get("/pautas/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Pauta 999999 não encontrada"))
                .andExpect(jsonPath("$.path").value("/pautas/999999"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    static Stream<String> corposInvalidos() {
        return Stream.of(
                "{\"descricao\": \"sem titulo\"}",
                "{\"titulo\": \"\"}",
                "{\"titulo\": \"   \"}",
                "{\"titulo\": \"" + "a".repeat(201) + "\"}",
                "{\"titulo\": \"ok\", \"descricao\": \"" + "d".repeat(2001) + "\"}",
                "{\"titulo\": \"ok\"",
                "isto não é json");
    }

    @ParameterizedTest
    @MethodSource("corposInvalidos")
    void dadosInvalidosRetornam400(String corpo) throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/pautas"));
    }

    @Test
    void tituloVazioRetornaFieldErrorsComCampoETitulo() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("", DESCRICAO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("titulo"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("não pode ser vazio"));
    }

    @Test
    void descricaoAcimaDoLimiteRetornaFieldErrorComCampoDescricao() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(TITULO, "d".repeat(2001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("descricao"));
    }

    @Test
    void campoDesconhecidoEIgnorado() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"" + TITULO + "\", \"campoExtra\": 1}"))
                .andExpect(status().isCreated());
    }

    @Test
    void tituloNoLimiteDe200EAceito() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("a".repeat(200), null)))
                .andExpect(status().isCreated());
    }

    @Test
    void descricaoNoLimiteDe2000EAceita() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(TITULO, "d".repeat(2000))))
                .andExpect(status().isCreated());
    }

    @Test
    void pautaDuplicadaEPermitida() throws Exception {
        long primeira = cadastrar(TITULO, null);
        long segunda = cadastrar(TITULO, null);
        org.junit.jupiter.api.Assertions.assertNotEquals(primeira, segunda);
    }

    static Stream<Arguments> requisicoesFormaInvalida() {
        return Stream.of(
                Arguments.of(get("/pautas/abc"), 400, "Bad Request", "Parâmetro inválido: id"),
                Arguments.of(post("/pautas").contentType(MediaType.TEXT_PLAIN).content("titulo"),
                        415, "Unsupported Media Type", "Tipo de mídia não suportado"),
                Arguments.of(put("/pautas"), 405, "Method Not Allowed", "Método não permitido"),
                Arguments.of(delete("/pautas/1"), 405, "Method Not Allowed", "Método não permitido"));
    }

    @ParameterizedTest
    @MethodSource("requisicoesFormaInvalida")
    void requisicaoForaDoContratoRetornaErroPadronizado(
            MockHttpServletRequestBuilder requisicao, int status, String error, String message) throws Exception {
        mvc.perform(requisicao)
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Test
    void tituloComTipoInvalidoRetornaFieldErrorNoCampoTitulo() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": [\"Aprovação\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição inválido"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("titulo"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("tipo inválido"));
    }

    @Test
    void jsonMalformadoNoCadastroRetornaFieldErrorsVazio() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição inválido"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Test
    void corpoEmArrayNoCadastroNaoGeraCampoNulo() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Test
    void idEnviadoNoPostNaoEhAceitoComoIdentificador() throws Exception {
        MvcResult resultado = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": 42, \"titulo\": \"" + TITULO + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String location = resultado.getResponse().getHeader("Location");
        org.junit.jupiter.api.Assertions.assertNotEquals("/pautas/42", location);
    }

    private long cadastrar(String titulo, String descricao) throws Exception {
        MvcResult resultado = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(titulo, descricao)))
                .andExpect(status().isCreated())
                .andReturn();
        String corpo = resultado.getResponse().getContentAsString();
        return Long.parseLong(corpo.replaceAll("\\D", ""));
    }

    private static String json(String titulo, String descricao) {
        String desc = descricao == null ? "null" : "\"" + descricao + "\"";
        return "{\"titulo\": \"" + titulo + "\", \"descricao\": " + desc + "}";
    }
}

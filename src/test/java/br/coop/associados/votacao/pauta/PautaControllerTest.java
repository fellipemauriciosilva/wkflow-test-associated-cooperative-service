package br.coop.associados.votacao.pauta;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.coop.associados.votacao.web.LimiteBodyFilter;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
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
    void cadastraPautaComIdGerado() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Aprovar contas\",\"descricao\":\"Contas do exercício\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.notNullValue()))
                .andExpect(jsonPath("$.titulo").value("Aprovar contas"))
                .andExpect(jsonPath("$.descricao").value("Contas do exercício"))
                .andReturn();
        long id = ((Number) JsonPath.read(criada.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    void cadastroRetornaHeaderLocationExato() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Location\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long id = ((Number) JsonPath.read(criada.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(criada.getResponse().getHeader("Location"))
                .isEqualTo("/pautas/" + id);
    }

    @Test
    void consultaPautaCadastradaPorId() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Eleger conselho\",\"descricao\":\"Mandato 2027\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long id = ((Number) JsonPath.read(criada.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Eleger conselho"))
                .andExpect(jsonPath("$.descricao").value("Mandato 2027"));
    }

    @Test
    void consultaIdInexistenteRetorna404NoFormatoPadrao() throws Exception {
        mvc.perform(get("/pautas/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Pauta não encontrada: 999999"))
                .andExpect(jsonPath("$.path").value("/pautas/999999"));
    }

    @Test
    void cadastroSemTituloRetorna400() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"sem titulo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("titulo")))
                .andExpect(jsonPath("$.path").value("/pautas"));
    }

    @Test
    void cadastroComTituloEmBrancoRetorna400() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("titulo")));
    }

    @Test
    void cadastroSemDescricaoEhAceitoComDescricaoNula() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Sem descricao\",\"descricao\":null}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value(nullValue()));
    }

    @Test
    void tituloNoLimiteExatoEhAceito() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("a".repeat(PautaRequest.TITULO_MAX), null)))
                .andExpect(status().isCreated());
    }

    @Test
    void tituloAcimaDoLimiteRetorna400() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("a".repeat(PautaRequest.TITULO_MAX + 1), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("titulo")));
    }

    @Test
    void descricaoNoLimiteExatoEhAceita() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Titulo", "d".repeat(PautaRequest.DESCRICAO_MAX))))
                .andExpect(status().isCreated());
    }

    @Test
    void descricaoAcimaDoLimiteRetorna400() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Titulo", "d".repeat(PautaRequest.DESCRICAO_MAX + 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("descricao")));
    }

    @Test
    void jsonMalformadoRetorna400NoFormatoPadrao() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Corpo da requisição inválido"))
                .andExpect(jsonPath("$.path").value("/pautas"));
    }

    @Test
    void idNaoNumericoRetorna400NoFormatoPadrao() throws Exception {
        mvc.perform(get("/pautas/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Parâmetro 'id' inválido"))
                .andExpect(jsonPath("$.path").value("/pautas/abc"));
    }

    @Test
    void corpoAcimaDoLimiteDeBytesRetorna413() throws Exception {
        String grande = "{\"titulo\":\"x\",\"descricao\":\"" + "d".repeat(20 * 1024) + "\"}";
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Content-Length", grande.getBytes(StandardCharsets.UTF_8).length)
                        .content(grande))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.error").value("Payload Too Large"))
                .andExpect(jsonPath("$.message").value(LimiteBodyFilter.MENSAGEM_LIMITE))
                .andExpect(jsonPath("$.path").value("/pautas"));
    }

    @Test
    void corpoChunkedDentroDoLimiteEhAceito() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Chunked\"}"))
                .andExpect(status().isCreated());
    }

    private static String corpo(String titulo, String descricao) {
        if (descricao == null) {
            return "{\"titulo\":\"" + titulo + "\"}";
        }
        return "{\"titulo\":\"" + titulo + "\",\"descricao\":\"" + descricao + "\"}";
    }
}

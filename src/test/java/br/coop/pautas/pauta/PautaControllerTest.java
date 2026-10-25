package br.coop.pautas.pauta;

import static org.assertj.core.api.Assertions.assertThat;
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

    private static final String PAYLOAD_VALIDO = """
            {"titulo": "Aprovar contas", "descricao": "Contas do exercicio"}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PautaRepository repository;

    @Test
    void criaPautaERetornaIdNaoNulo() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/pautas/")))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void consultaPautaExistenteRetornaCamposEnviados() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO))
                .andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(criada.getResponse().getContentAsString(), "$.id").toString();

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Long.parseLong(id)))
                .andExpect(jsonPath("$.titulo").value("Aprovar contas"))
                .andExpect(jsonPath("$.descricao").value("Contas do exercicio"));
    }

    @Test
    void consultaPautaInexistenteRetorna404ComProblemDetail() throws Exception {
        mvc.perform(get("/pautas/{id}", 999_999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void criaPautaSemTituloRetorna400ENaoPersiste() throws Exception {
        long antes = repository.count();

        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\": \"sem titulo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"   \"}"))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isEqualTo(antes);
    }

    @Test
    void pautaCriadaEstaPersistidaEControlaPeloId() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO))
                .andReturn();
        long id = com.jayway.jsonpath.JsonPath.parse(criada.getResponse().getContentAsString())
                .read("$.id", Long.class);

        Pauta persistida = repository.findById(id).orElseThrow();
        assertThat(persistida.getTitulo()).isEqualTo("Aprovar contas");
        assertThat(persistida.getDescricao()).isEqualTo("Contas do exercicio");
    }
}

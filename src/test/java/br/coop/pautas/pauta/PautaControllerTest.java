package br.coop.pautas.pauta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PautaControllerTest {

    private static final String PAYLOAD_VALIDO = """
            {"titulo": "Aprovar contas", "descricao": "Contas do exercicio"}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PautaRepository repository;

    @BeforeEach
    void limpaBase() {
        repository.deleteAll();
    }

    @Test
    void criaPautaERetornaIdNaoNulo() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        String id = JsonPath.read(criada.getResponse().getContentAsString(), "$.id").toString();
        assertThat(criada.getResponse().getHeader("Location")).isEqualTo("/pautas/" + id);
    }

    @Test
    void consultaPautaExistenteRetornaCamposEnviados() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO))
                .andReturn();
        String id = JsonPath.read(criada.getResponse().getContentAsString(), "$.id").toString();

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Long.parseLong(id)))
                .andExpect(jsonPath("$.titulo").value("Aprovar contas"))
                .andExpect(jsonPath("$.descricao").value("Contas do exercicio"));
    }

    @Test
    void criaPautaSoComTituloERetornaDescricaoNula() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"So titulo\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id = JsonPath.read(criada.getResponse().getContentAsString(), "$.id").toString();

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("So titulo"))
                .andExpect(jsonPath("$.descricao").value(nullValue()));
    }

    @Test
    void consultaPautaInexistenteRetorna404ComProblemDetail() throws Exception {
        mvc.perform(get("/pautas/{id}", 999_999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void criaPautaSemTituloRetorna400ENaoPersiste() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\": \"sem titulo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \" \"}"))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @Test
    void criaPautaComTituloDe256CaracteresRetorna400ENaoPersiste() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"" + "a".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertThat(repository.count()).isZero();
    }

    @Test
    void criaPautaComDescricaoDe256CaracteresRetorna400ENaoPersiste() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Ok\", \"descricao\": \"" + "d".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertThat(repository.count()).isZero();
    }

    @Test
    void pautaCriadaEstaPersistidaComCamposCorretos() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO))
                .andReturn();
        Long id = ((Number) JsonPath.read(criada.getResponse().getContentAsString(), "$.id")).longValue();

        Pauta persistida = repository.findById(id).orElseThrow();
        assertThat(persistida.getTitulo()).isEqualTo("Aprovar contas");
        assertThat(persistida.getDescricao()).isEqualTo("Contas do exercicio");
    }

    @Test
    void criaPautaComTituloDe255CaracteresRetorna201ComTamanhoPreservado() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"" + "t".repeat(255) + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = ((Number) JsonPath.read(criada.getResponse().getContentAsString(), "$.id")).longValue();

        assertThat(repository.findById(id).orElseThrow().getTitulo()).hasSize(255);
    }

    @Test
    void criaPautaComDescricaoDe255CaracteresRetorna201ComTamanhoPreservado() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Ok\", \"descricao\": \"" + "d".repeat(255) + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = ((Number) JsonPath.read(criada.getResponse().getContentAsString(), "$.id")).longValue();

        assertThat(repository.findById(id).orElseThrow().getDescricao()).hasSize(255);
    }

    @Test
    void corpoAcimaDoLimiteRetorna413SemPersistir() throws Exception {
        String corpo = "{\"titulo\": \"Ok\", \"descricao\": \"" + "x".repeat(10_000) + "\"}";

        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isPayloadTooLarge());

        assertThat(repository.count()).isZero();
    }
}

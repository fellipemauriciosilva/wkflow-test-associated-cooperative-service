package br.coop.assembleia.pauta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class PautaControllerTest {

    private static final String NBSP = "\u00A0";
    private static final String ZERO_WIDTH_SPACE = "\u200B";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PautaRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void limparBase() {
        repository.deleteAll();
    }

    @Test
    void criaPautaComTituloValidoRetorna201ComId() throws Exception {
        postPauta(Map.of("titulo", "Aprovar balanço", "descricao", "Balanço 2025"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void semDescricaoEAceitaComStatus201() throws Exception {
        postPauta(Map.of("titulo", "Eleição do conselho"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void semTituloComDescricaoRetorna400SemPersistir() throws Exception {
        postPauta(Map.of("descricao", "Somente descricao"))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            NBSP,
            ZERO_WIDTH_SPACE,
            ZERO_WIDTH_SPACE + " " + NBSP + ZERO_WIDTH_SPACE
    })
    void tituloVazioOuSemCaractereVisivelRetorna400SemPersistir(String titulo) throws Exception {
        postPauta(Map.of("titulo", titulo))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @Test
    void tituloComDuzentosCinquentaECincoCaracteresEAceito() throws Exception {
        postPauta(Map.of("titulo", "a".repeat(255)))
                .andExpect(status().isCreated());
    }

    @Test
    void tituloComDuzentosCinquentaESeisCaracteresRetorna400SemPersistir() throws Exception {
        postPauta(Map.of("titulo", "a".repeat(256)))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @Test
    void descricaoComDuzentosCinquentaESeisCaracteresRetorna400SemPersistir() throws Exception {
        Map<String, String> corpo = new HashMap<>();
        corpo.put("titulo", "Aprovar estatuto");
        corpo.put("descricao", "d".repeat(256));

        postPauta(corpo).andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @Test
    void buscaPautaExistenteRetorna200ComTituloEDescricao() throws Exception {
        String corpo = postPauta(Map.of("titulo", "Aprovar estatuto", "descricao", "Revisao do art. 10"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(corpo).get("id").asLong();

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Aprovar estatuto"))
                .andExpect(jsonPath("$.descricao").value("Revisao do art. 10"));
    }

    @Test
    void buscaPautaRemovidaRetorna404() throws Exception {
        String corpo = postPauta(Map.of("titulo", "Pauta temporaria"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(corpo).get("id").asLong();
        repository.deleteById(id);

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isNotFound());
    }

    private ResultActions postPauta(Object corpo) throws Exception {
        return mvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpo)));
    }
}

package br.coop.assembleia.pauta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PautaControllerTest {

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
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Aprovar balanço\",\"descricao\":\"Balanço 2025\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void semDescricaoEAceitaComStatus201() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Eleição do conselho\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void semTituloComDescricaoRetorna400SemPersistir() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Somente descricao\"}"))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void tituloVazioOuSoEspacosRetorna400SemPersistir(String titulo) throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"" + titulo + "\"}"))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @Test
    void buscaPautaExistenteRetorna200ComTituloEDescricao() throws Exception {
        String corpo = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Aprovar estatuto\",\"descricao\":\"Revisao do art. 10\"}"))
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
    void buscaPautaInexistenteRetorna404() throws Exception {
        mvc.perform(get("/pautas/{id}", 999_999L))
                .andExpect(status().isNotFound());
    }
}

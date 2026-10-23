package br.coop.cooperative.pauta;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PautaControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PautaRepository repository;

    @BeforeEach
    void limparBase() {
        repository.deleteAll();
    }

    @Test
    void criaPautaComTituloERetorna201ComId() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content("{\"titulo\": \"Pauta A\", \"descricao\": \"Desc\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    void criaPautaSemDescricaoRetorna201() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content("{\"titulo\": \"Pauta A\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\\t"})
    void tituloAusenteVazioOuEmBrancoRetorna400(String titulo) throws Exception {
        String corpo = titulo == null
                ? "{\"descricao\": \"Desc\"}"
                : "{\"titulo\": \"" + titulo + "\", \"descricao\": \"Desc\"}";

        mvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content(corpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buscaPautaExistenteRetorna200ComCampos() throws Exception {
        mvc.perform(post("/pautas")
                .contentType("application/json")
                .content("{\"titulo\": \"Pauta B\", \"descricao\": \"Detalhe\"}"));
        Long id = repository.findAll().get(0).getId();

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Pauta B"))
                .andExpect(jsonPath("$.descricao").value("Detalhe"));
    }

    @Test
    void buscaPautaInexistenteRetorna404() throws Exception {
        mvc.perform(get("/pautas/{id}", 999999L))
                .andExpect(status().isNotFound());
    }
}

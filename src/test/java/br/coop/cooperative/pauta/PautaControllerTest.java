package br.coop.cooperative.pauta;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

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

    @Test
    void descricaoComMil_caracteresRetorna201() throws Exception {
        String descricao = "d".repeat(1000);
        mvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content("{\"titulo\": \"Pauta A\", \"descricao\": \"" + descricao + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void descricaoComMilUmCaracteresRetorna400() throws Exception {
        String descricao = "d".repeat(1001);
        mvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content("{\"titulo\": \"Pauta A\", \"descricao\": \"" + descricao + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(
                        MethodArgumentNotValidException.class, result.getResolvedException()));
    }

    @Test
    void tituloComDuzentosUmCaracteresRetorna400() throws Exception {
        String titulo = "t".repeat(201);
        mvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content("{\"titulo\": \"" + titulo + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(
                        MethodArgumentNotValidException.class, result.getResolvedException()));
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
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(
                        MethodArgumentNotValidException.class, result.getResolvedException()));
    }

    @Test
    void buscaPautaExistenteRetorna200ComCampos() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content("{\"titulo\": \"Pauta B\", \"descricao\": \"Detalhe\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(criada.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Pauta B"))
                .andExpect(jsonPath("$.descricao").value("Detalhe"));
    }

    @Test
    void buscaPautaInexistenteRetorna404DoHandler() throws Exception {
        MvcResult resultado = mvc.perform(get("/pautas/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andReturn();

        ResponseStatusException erro = assertInstanceOf(
                ResponseStatusException.class, resultado.getResolvedException());
        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }
}

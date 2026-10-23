package br.coop.assembleia.pauta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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

@SpringBootTest
@AutoConfigureMockMvc
class PautaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PautaRepository repository;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    @Test
    void criaPautaComTituloValidoRetorna201ComId() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content("{\"titulo\":\"Aprovar balanco\",\"descricao\":\"Balanco 2025\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.titulo").value("Aprovar balanco"))
                .andExpect(jsonPath("$.descricao").value("Balanco 2025"))
                .andReturn();

        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        assertThat(resultado.getResponse().getHeader("Location")).isEqualTo("/pautas/" + id);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void criaPautaSemDescricaoRetorna201() throws Exception {
        mockMvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content("{\"titulo\":\"Aprovar balanco\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\\t"})
    void rejeitaTituloNuloVazioOuSoEspacosSemPersistir(String titulo) throws Exception {
        String corpo = titulo == null
                ? "{\"descricao\":\"sem titulo\"}"
                : "{\"titulo\":\"" + titulo + "\"}";

        mockMvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content(corpo))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @Test
    void rejeitaTituloAcimaDoLimiteSemPersistir() throws Exception {
        String titulo = "a".repeat(Pauta.TITULO_MAX + 1);

        mockMvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content("{\"titulo\":\"" + titulo + "\"}"))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @Test
    void rejeitaDescricaoAcimaDoLimiteSemPersistir() throws Exception {
        String descricao = "d".repeat(Pauta.DESCRICAO_MAX + 1);

        mockMvc.perform(post("/pautas")
                        .contentType("application/json")
                        .content("{\"titulo\":\"Aprovar balanco\",\"descricao\":\"" + descricao + "\"}"))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @Test
    void buscaPautaExistenteRetorna200ComCamposPreenchidos() throws Exception {
        Pauta salva = repository.save(new Pauta("Eleger conselho", "Mandato 2026-2028"));

        mockMvc.perform(get("/pautas/{id}", salva.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(salva.getId()))
                .andExpect(jsonPath("$.titulo").value("Eleger conselho"))
                .andExpect(jsonPath("$.descricao").value("Mandato 2026-2028"));
    }

    @Test
    void buscaPautaInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/pautas/{id}", 999_999L))
                .andExpect(status().isNotFound());
    }
}

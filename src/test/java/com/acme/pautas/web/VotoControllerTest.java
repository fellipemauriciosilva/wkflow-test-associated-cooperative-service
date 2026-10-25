package com.acme.pautas.web;

import com.acme.pautas.RelogioDeTeste;
import com.acme.pautas.repository.VotoRepository;
import com.acme.pautas.service.PautaService;
import com.acme.pautas.service.SessaoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RelogioDeTeste.class)
class VotoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PautaService pautas;

    @Autowired
    private SessaoService sessoes;

    @Autowired
    private VotoRepository votos;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private RelogioDeTeste.RelogioControlavel relogio;

    @BeforeEach
    void relogioNoInicio() {
        relogio.ajustarPara(RelogioDeTeste.INICIO);
    }

    @Test
    void registraVotoEmSessaoAberta() throws Exception {
        Long pautaId = pautaComSessao("Pauta aberta", 5);

        mvc.perform(votar(pautaId, "{\"associadoId\": \"a1\", \"voto\": \"SIM\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pautaId").value(pautaId))
                .andExpect(jsonPath("$.associadoId").value("a1"))
                .andExpect(jsonPath("$.voto").value("SIM"));

        assertEquals(1, votos.countByPautaId(pautaId));
    }

    @Test
    void rejeitaVotoSemSessaoAberta() throws Exception {
        Long pautaId = pautas.cadastrar("Pauta sem sessão", null).getId();

        mvc.perform(votar(pautaId, "{\"associadoId\": \"a1\", \"voto\": \"SIM\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("CONFLITO"));

        assertEquals(0, votos.countByPautaId(pautaId));
    }

    @Test
    void rejeitaVotoAposFimDaSessao() throws Exception {
        Long pautaId = pautaComSessao("Pauta expirada", 1);
        relogio.ajustarPara(RelogioDeTeste.INICIO.plus(Duration.ofMinutes(1)));

        mvc.perform(votar(pautaId, "{\"associadoId\": \"a1\", \"voto\": \"SIM\"}"))
                .andExpect(status().isConflict());

        assertEquals(0, votos.countByPautaId(pautaId));
    }

    @Test
    void rejeitaSegundoVotoDoMesmoAssociado() throws Exception {
        Long pautaId = pautaComSessao("Pauta voto duplicado", 5);
        String corpo = "{\"associadoId\": \"a1\", \"voto\": \"SIM\"}";

        mvc.perform(votar(pautaId, corpo)).andExpect(status().isCreated());
        mvc.perform(votar(pautaId, "{\"associadoId\": \"a1\", \"voto\": \"NAO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("CONFLITO"));

        assertEquals(1, votos.countByPautaId(pautaId));
    }

    @Test
    void rejeitaVotoInvalido() throws Exception {
        Long pautaId = pautaComSessao("Pauta voto inválido", 5);

        mvc.perform(votar(pautaId, "{\"associadoId\": \"a1\", \"voto\": \"TALVEZ\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"));

        assertEquals(0, votos.countByPautaId(pautaId));
    }

    @Test
    void rejeitaVotoAusente() throws Exception {
        Long pautaId = pautaComSessao("Pauta voto ausente", 5);

        mvc.perform(votar(pautaId, "{\"associadoId\": \"a1\"}"))
                .andExpect(status().isBadRequest());

        assertEquals(0, votos.countByPautaId(pautaId));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"associadoId\": \"\", \"voto\": \"SIM\"}", "{\"voto\": \"SIM\"}"})
    void rejeitaAssociadoVazioOuAusente(String corpo) throws Exception {
        Long pautaId = pautaComSessao("Pauta associado " + corpo.length(), 5);

        mvc.perform(votar(pautaId, corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"));

        assertEquals(0, votos.countByPautaId(pautaId));
    }

    @Test
    void rejeitaVotoEmPautaInexistente() throws Exception {
        mvc.perform(votar(999_999L, "{\"associadoId\": \"a1\", \"voto\": \"SIM\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.path").value("/pautas/999999/votos"));
    }

    @Test
    void resultadoContaSimENaoComVotos() throws Exception {
        Long pautaId = pautaComSessao("Pauta resultado", 5);
        mvc.perform(votar(pautaId, "{\"associadoId\": \"a1\", \"voto\": \"SIM\"}")).andExpect(status().isCreated());
        mvc.perform(votar(pautaId, "{\"associadoId\": \"a2\", \"voto\": \"SIM\"}")).andExpect(status().isCreated());
        mvc.perform(votar(pautaId, "{\"associadoId\": \"a3\", \"voto\": \"NAO\"}")).andExpect(status().isCreated());

        mvc.perform(get("/pautas/{id}/resultado", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sim").value(2))
                .andExpect(jsonPath("$.nao").value(1));
    }

    @Test
    void resultadoDePautaSemVotosEhZero() throws Exception {
        Long pautaId = pautas.cadastrar("Pauta sem votos", null).getId();

        mvc.perform(get("/pautas/{id}/resultado", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sim").value(0))
                .andExpect(jsonPath("$.nao").value(0));
    }

    @Test
    void resultadoComSessaoAbertaTemApenasChavesSimENao() throws Exception {
        Long pautaId = pautaComSessao("Pauta chaves", 5);
        mvc.perform(votar(pautaId, "{\"associadoId\": \"a1\", \"voto\": \"NAO\"}")).andExpect(status().isCreated());

        MvcResult resultado = mvc.perform(get("/pautas/{id}/resultado", pautaId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode corpo = mapper.readTree(resultado.getResponse().getContentAsString());
        Set<String> chaves = new TreeSet<>();
        Iterator<String> nomes = corpo.fieldNames();
        nomes.forEachRemaining(chaves::add);
        assertEquals(Set.of("sim", "nao"), chaves);
    }

    @Test
    void resultadoDePautaInexistenteRetorna404() throws Exception {
        mvc.perform(get("/pautas/{id}/resultado", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.path").value("/pautas/999999/resultado"));
    }

    private Long pautaComSessao(String titulo, int duracaoMinutos) {
        Long pautaId = pautas.cadastrar(titulo, null).getId();
        sessoes.abrir(pautaId, duracaoMinutos);
        return pautaId;
    }

    private org.springframework.test.web.servlet.RequestBuilder votar(Long pautaId, String corpo) {
        return post("/pautas/{id}/votos", pautaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo);
    }
}

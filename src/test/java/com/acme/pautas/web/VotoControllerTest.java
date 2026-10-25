package com.acme.pautas.web;

import com.acme.pautas.RelogioDeTeste;
import com.acme.pautas.domain.OpcaoVoto;
import com.acme.pautas.domain.Voto;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "pautas.votos.habilitado=true")
@ActiveProfiles("test")
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

        List<Voto> gravados = votosDaPauta(pautaId);
        assertEquals(1, gravados.size());
        assertEquals(com.acme.pautas.domain.OpcaoVoto.SIM, gravados.get(0).getOpcao());
    }

    @Test
    void mesmoAssociadoPodeVotarEmPautasDiferentes() throws Exception {
        Long primeiraPauta = pautaComSessao("Pauta A", 5);
        Long segundaPauta = pautaComSessao("Pauta B", 5);

        mvc.perform(votar(primeiraPauta, "{\"associadoId\": \"a1\", \"voto\": \"SIM\"}"))
                .andExpect(status().isCreated());
        mvc.perform(votar(segundaPauta, "{\"associadoId\": \"a1\", \"voto\": \"NAO\"}"))
                .andExpect(status().isCreated());

        assertEquals(1, votos.countByPautaId(primeiraPauta));
        assertEquals(1, votos.countByPautaId(segundaPauta));
    }

    @Test
    void associadoIdComEspacosNasPontasEhTratadoComoOMesmoAssociado() throws Exception {
        Long pautaId = pautaComSessao("Pauta normalização", 5);

        mvc.perform(votar(pautaId, "{\"associadoId\": \"a1\", \"voto\": \"SIM\"}"))
                .andExpect(status().isCreated());
        mvc.perform(votar(pautaId, "{\"associadoId\": \"a1 \", \"voto\": \"SIM\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("CONFLITO"));

        assertEquals(1, votos.countByPautaId(pautaId));
    }

    @Test
    void associadoIdEmNFDEhTratadoComoOMesmoQueNFC() throws Exception {
        Long pautaId = pautaComSessao("Pauta unicode", 5);

        mvc.perform(votar(pautaId, "{\"associadoId\": \"Jose\u0301\", \"voto\": \"SIM\"}"))
                .andExpect(status().isCreated());
        mvc.perform(votar(pautaId, "{\"associadoId\": \"Jos\u00e9\", \"voto\": \"SIM\"}"))
                .andExpect(status().isConflict());

        assertEquals(1, votos.countByPautaId(pautaId));
    }

    @Test
    void associadoIdComMaiusculaEhOutroAssociado() throws Exception {
        Long pautaId = pautaComSessao("Pauta caixa", 5);

        mvc.perform(votar(pautaId, "{\"associadoId\": \"a1\", \"voto\": \"SIM\"}"))
                .andExpect(status().isCreated());
        mvc.perform(votar(pautaId, "{\"associadoId\": \"A1\", \"voto\": \"SIM\"}"))
                .andExpect(status().isCreated());

        assertEquals(2, votos.countByPautaId(pautaId));
    }

    @Test
    void associadoIdCom256CaracteresRetorna400() throws Exception {
        Long pautaId = pautaComSessao("Pauta tamanho 256", 5);

        mvc.perform(votar(pautaId, corpoComAssociado("a".repeat(256))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"));

        assertEquals(0, votos.countByPautaId(pautaId));
    }

    @Test
    void associadoIdCom255CaracteresEhAceito() throws Exception {
        Long pautaId = pautaComSessao("Pauta tamanho 255", 5);

        mvc.perform(votar(pautaId, corpoComAssociado("a".repeat(255))))
                .andExpect(status().isCreated());

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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"));

        assertEquals(0, votos.countByPautaId(pautaId));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"associadoId\": \"\", \"voto\": \"SIM\"}",
            "{\"associadoId\": \"   \", \"voto\": \"SIM\"}",
            "{\"voto\": \"SIM\"}"})
    void rejeitaAssociadoVazioOuAusente(String corpo) throws Exception {
        Long pautaId = pautaComSessao("Pauta associado " + corpo.length(), 5);

        mvc.perform(votar(pautaId, corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"));

        assertEquals(0, votos.countByPautaId(pautaId));
    }

    @Test
    void corpoInvalidoTemPrecedenciaSobrePautaInexistente() throws Exception {
        mvc.perform(votar(999_999L, "{\"associadoId\": \"a1\", \"voto\": \"TALVEZ\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"));
    }

    @Test
    void corpoInvalidoTemPrecedenciaSobreSessaoAusente() throws Exception {
        Long pautaId = pautas.cadastrar("Pauta corpo antes de sessão", null).getId();

        mvc.perform(votar(pautaId, "{\"associadoId\": \"a1\", \"voto\": \"TALVEZ\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"));

        assertEquals(0, votos.countByPautaId(pautaId));
    }

    @Test
    void rejeitaVotoDuplicadoComConflito() throws Exception {
        Long pautaId = pautaComSessao("Pauta voto duplicado", 5);
        votos.saveAndFlush(new Voto(pautaId, "a1", OpcaoVoto.SIM));

        mvc.perform(votar(pautaId, "{\"associadoId\": \"a1\", \"voto\": \"NAO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("CONFLITO"));

        assertEquals(1, votos.countByPautaId(pautaId));
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
    void resultadoIgnoraVotosDeOutraPauta() throws Exception {
        Long pautaAlvo = pautaComSessao("Pauta alvo", 5);
        Long outraPauta = pautaComSessao("Pauta outra", 5);
        mvc.perform(votar(pautaAlvo, "{\"associadoId\": \"a1\", \"voto\": \"SIM\"}")).andExpect(status().isCreated());
        mvc.perform(votar(outraPauta, "{\"associadoId\": \"a2\", \"voto\": \"SIM\"}")).andExpect(status().isCreated());
        mvc.perform(votar(outraPauta, "{\"associadoId\": \"a3\", \"voto\": \"NAO\"}")).andExpect(status().isCreated());

        mvc.perform(get("/pautas/{id}/resultado", pautaAlvo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sim").value(1))
                .andExpect(jsonPath("$.nao").value(0));
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
    void resultadoTemApenasChavesSimENaoNoJson() throws Exception {
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

    private List<Voto> votosDaPauta(Long pautaId) {
        return votos.findAll().stream().filter(v -> v.getPautaId().equals(pautaId)).toList();
    }

    private static String corpoComAssociado(String associadoId) {
        return "{\"associadoId\": \"" + associadoId + "\", \"voto\": \"SIM\"}";
    }

    @Test
    void associadoIdDe255CaracteresComEspacosNasPontasEhAceito() throws Exception {
        Long pautaId = pautaComSessao("Pauta limite 255", 5);
        String associado = "   " + "a".repeat(255) + "   ";

        mvc.perform(votar(pautaId, "{\"associadoId\": \"" + associado + "\", \"voto\": \"SIM\"}"))
                .andExpect(status().isCreated());

        assertEquals(1, votos.countByPautaId(pautaId));
    }

    @Test
    void associadoIdDe256CaracteresComEspacosNasPontasEhRejeitado() throws Exception {
        Long pautaId = pautaComSessao("Pauta limite 256", 5);
        String associado = "  " + "b".repeat(256) + "  ";

        mvc.perform(votar(pautaId, "{\"associadoId\": \"" + associado + "\", \"voto\": \"SIM\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"));

        assertEquals(0, votos.countByPautaId(pautaId));
    }

    private org.springframework.test.web.servlet.RequestBuilder votar(Long pautaId, String corpo) {
        return post("/pautas/{id}/votos", pautaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo);
    }
}

package pautas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import jakarta.servlet.ServletException;

@SpringBootTest
@AutoConfigureMockMvc
class SessaoControllerTest {

    private static final Instant INICIO = Instant.parse("2025-01-01T10:00:00Z");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ClockDeTeste clock;

    @SpyBean
    private SessaoRepository sessaoRepository;

    @AfterEach
    void reiniciarRelogio() {
        clock.reiniciar();
    }

    @Test
    void semDuracaoAbreSessaoDeUmMinuto() throws Exception {
        long pautaId = cadastrarPauta();

        MvcResult resultado = mvc.perform(post("/pautas/{id}/sessao", pautaId))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/pautas/" + pautaId + "/sessao"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.pautaId").value(pautaId))
                .andReturn();

        assertThat(duracao(resultado)).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void corpoVazioAbreSessaoDeUmMinuto() throws Exception {
        long pautaId = cadastrarPauta();

        MvcResult resultado = mvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(duracao(resultado)).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void duracaoInformadaEhRespeitada() throws Exception {
        long pautaId = cadastrarPauta();

        MvcResult resultado = mvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\": 5}"))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(duracao(resultado)).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void aberturaVemDoRelogioInjetado() throws Exception {
        long pautaId = cadastrarPauta();

        MvcResult resultado = mvc.perform(post("/pautas/{id}/sessao", pautaId))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(Instant.parse(json(resultado).get("abertura").asText())).isEqualTo(INICIO);
    }

    @Test
    void pautaInexistenteRetorna404() throws Exception {
        mvc.perform(post("/pautas/{id}/sessao", 999_999_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void pautaComSessaoRetorna409() throws Exception {
        long pautaId = cadastrarPauta();
        mvc.perform(post("/pautas/{id}/sessao", pautaId)).andExpect(status().isCreated());

        mvc.perform(post("/pautas/{id}/sessao", pautaId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(mensagemConflito(pautaId)));
    }

    @Test
    void pautaComSessaoExpiradaContinua409() throws Exception {
        long pautaId = cadastrarPauta();
        mvc.perform(post("/pautas/{id}/sessao", pautaId)).andExpect(status().isCreated());
        clock.avancar(Duration.ofMinutes(2));

        mvc.perform(post("/pautas/{id}/sessao", pautaId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(mensagemConflito(pautaId)));
    }

    @Test
    void corridaEntreAberturasEBarradaPelaConstraintUnica() throws Exception {
        long pautaId = cadastrarPauta();
        mvc.perform(post("/pautas/{id}/sessao", pautaId)).andExpect(status().isCreated());
        doReturn(false).when(sessaoRepository).existsByPautaId(any());

        mvc.perform(post("/pautas/{id}/sessao", pautaId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(mensagemConflito(pautaId)));
    }

    @Test
    void violacaoNaoRelacionadaAUnicidadeNaoViraConflito() throws Exception {
        long pautaId = cadastrarPauta();
        doThrow(new DataIntegrityViolationException("outra violacao")).when(sessaoRepository).saveAndFlush(any());

        assertThatThrownBy(() -> mvc.perform(post("/pautas/{id}/sessao", pautaId)))
                .isInstanceOf(ServletException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @MethodSource("duracoesInvalidas")
    void duracaoInvalidaRetorna400ComErroNoCampo(String corpo) throws Exception {
        long pautaId = cadastrarPauta();

        mvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("duracaoMinutos"));
    }

    static Stream<Arguments> duracoesInvalidas() {
        return Stream.of(
                Arguments.of("{\"duracaoMinutos\": 0}"),
                Arguments.of("{\"duracaoMinutos\": -1}"),
                Arguments.of("{\"duracaoMinutos\": \"abc\"}"),
                Arguments.of("{\"duracaoMinutos\": 1.5}"),
                Arguments.of("{\"duracaoMinutos\": \"5\"}"),
                Arguments.of("{\"duracaoMinutos\": 1e2}"));
    }

    @Test
    void duracaoZeroOuNegativaTemMensagemDeMinimo() throws Exception {
        long pautaId = cadastrarPauta();

        mvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].message").value("deve ser um inteiro maior que zero"));
    }

    @Test
    void duracaoNaoNumericaTemMensagemDeTipo() throws Exception {
        long pautaId = cadastrarPauta();

        mvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\": \"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].message").value("tipo inválido"));
    }

    @Test
    void jsonMalformadoRetornaFieldErrorsVazio() throws Exception {
        long pautaId = cadastrarPauta();

        mvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void corpoEmArrayNaoGeraCampoNulo() throws Exception {
        long pautaId = cadastrarPauta();

        mvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    private long cadastrarPauta() throws Exception {
        MvcResult resultado = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Pauta de sessão\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return json(resultado).get("id").asLong();
    }

    private JsonNode json(MvcResult resultado) throws Exception {
        return mapper.readTree(resultado.getResponse().getContentAsString());
    }

    private Duration duracao(MvcResult resultado) throws Exception {
        JsonNode corpo = json(resultado);
        Instant abertura = Instant.parse(corpo.get("abertura").asText());
        Instant fim = Instant.parse(corpo.get("fim").asText());
        return Duration.between(abertura, fim);
    }

    private static String mensagemConflito(long pautaId) {
        return "Pauta " + pautaId + " já possui sessão de votação";
    }

    static class ClockDeTeste extends Clock {

        private Instant instante = INICIO;

        void reiniciar() {
            instante = INICIO;
        }

        void avancar(Duration duracao) {
            instante = instante.plus(duracao);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }

    @TestConfiguration
    static class ConfigClockDeTeste {

        @Bean
        @Primary
        ClockDeTeste clockDeTeste() {
            return new ClockDeTeste();
        }
    }
}

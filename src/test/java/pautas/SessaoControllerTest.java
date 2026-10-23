package pautas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class SessaoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void semDuracaoAbreSessaoDeUmMinuto() throws Exception {
        long pautaId = cadastrarPauta();

        MvcResult resultado = mvc.perform(post("/pautas/{id}/sessao", pautaId))
                .andExpect(status().isCreated())
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
                .andExpect(jsonPath("$.status").value(409));
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
                Arguments.of("{\"duracaoMinutos\": \"abc\"}"));
    }

    private long cadastrarPauta() throws Exception {
        MvcResult resultado = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Pauta de sessão\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private Duration duracao(MvcResult resultado) throws Exception {
        JsonNode corpo = mapper.readTree(resultado.getResponse().getContentAsString());
        Instant abertura = Instant.parse(corpo.get("abertura").asText());
        Instant fim = Instant.parse(corpo.get("fim").asText());
        return Duration.between(abertura, fim);
    }
}

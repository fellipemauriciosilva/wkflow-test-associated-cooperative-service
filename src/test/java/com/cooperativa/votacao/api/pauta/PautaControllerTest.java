package com.cooperativa.votacao.api.pauta;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cooperativa.votacao.api.TamanhoMaximoBodyFilter;
import com.cooperativa.votacao.domain.pauta.Pauta;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PautaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /pautas com dados validos retorna 201 com id")
    void criar_comDadosValidos_retorna201ComId() throws Exception {
        var payload = """
                {"titulo": "Eleicao de diretoria", "descricao": "Votacao anual"}
                """;

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.titulo").value("Eleicao de diretoria"))
                .andExpect(jsonPath("$.descricao").value("Votacao anual"));
    }

    @Test
    @DisplayName("POST /pautas sem titulo retorna 400")
    void criar_semTitulo_retorna400() throws Exception {
        var payload = """
                {"descricao": "Votacao anual"}
                """;

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value("titulo e obrigatorio"));
    }

    @Test
    @DisplayName("POST /pautas com titulo em branco retorna 400")
    void criar_comTituloEmBranco_retorna400() throws Exception {
        var payload = """
                {"titulo": "   ", "descricao": "Votacao anual"}
                """;

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value("titulo e obrigatorio"));
    }

    @Test
    @DisplayName("POST /pautas com titulo de 201 caracteres retorna 400 com mensagem exata")
    void criar_comTituloAcimaDoLimite_retorna400() throws Exception {
        var tituloLongo = "a".repeat(Pauta.TITULO_TAMANHO_MAXIMO + 1);
        var payload = objectMapper.writeValueAsString(new PautaRequestTeste(tituloLongo, "descricao qualquer"));

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem")
                        .value("titulo deve ter no maximo " + Pauta.TITULO_TAMANHO_MAXIMO + " caracteres"));
    }

    @Test
    @DisplayName("POST /pautas com descricao de 1001 caracteres retorna 400 com mensagem exata")
    void criar_comDescricaoAcimaDoLimite_retorna400() throws Exception {
        var descricaoLonga = "a".repeat(Pauta.DESCRICAO_TAMANHO_MAXIMO + 1);
        var payload = objectMapper.writeValueAsString(new PautaRequestTeste("titulo valido", descricaoLonga));

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem")
                        .value("descricao deve ter no maximo " + Pauta.DESCRICAO_TAMANHO_MAXIMO + " caracteres"));
    }

    @Test
    @DisplayName("POST /pautas com descricao de exatamente 1000 caracteres retorna 201")
    void criar_comDescricaoNoLimite_retorna201() throws Exception {
        var descricaoNoLimite = "a".repeat(Pauta.DESCRICAO_TAMANHO_MAXIMO);
        var payload = objectMapper.writeValueAsString(new PautaRequestTeste("titulo valido", descricaoNoLimite));

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value(descricaoNoLimite));
    }

    @Test
    @DisplayName("POST /pautas com JSON malformado retorna 400 com ErroResponse")
    void criar_comJsonMalformado_retorna400() throws Exception {
        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value("corpo da requisicao invalido ou ausente"));
    }

    @Test
    @DisplayName("GET /pautas/{id} com id nao numerico retorna 400 com ErroResponse")
    void buscarPorId_comIdNaoNumerico_retorna400() throws Exception {
        mockMvc.perform(get("/pautas/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value("parametro 'id' invalido"));
    }

    @Test
    @DisplayName("POST /pautas com body acima do limite retorna 413 com ErroResponse")
    void criar_comBodyMuitoGrande_retorna413() throws Exception {
        var descricaoEnorme = "a".repeat(20_000);
        var payload = objectMapper.writeValueAsString(new PautaRequestTeste("titulo valido", descricaoEnorme));

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.mensagem")
                        .value("corpo da requisicao excede o tamanho maximo permitido"));
    }

    @Test
    @DisplayName("POST /pautas com body 1 byte acima do limite retorna 413")
    void criar_comBodyUmByteAcimaDoLimite_retorna413() throws Exception {
        var prefixo = "{\"titulo\":\"titulo valido\",\"descricao\":\"";
        var sufixo = "\"}";
        var padding = (int) TamanhoMaximoBodyFilter.TAMANHO_MAXIMO_BYTES - prefixo.length() - sufixo.length() + 1;
        var payload = prefixo + "a".repeat(padding) + sufixo;

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413));
    }

    @Test
    @DisplayName("GET /pautas/{id} com pauta existente retorna 200")
    void buscarPorId_comPautaExistente_retorna200() throws Exception {
        var payload = """
                {"titulo": "Eleicao de diretoria", "descricao": "Votacao anual"}
                """;

        var resultadoCriacao = mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        var idCriado = objectMapper.readTree(resultadoCriacao.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/pautas/{id}", idCriado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idCriado))
                .andExpect(jsonPath("$.titulo").value("Eleicao de diretoria"));
    }

    @Test
    @DisplayName("GET /pautas/{id} com pauta inexistente retorna 404")
    void buscarPorId_comPautaInexistente_retorna404() throws Exception {
        mockMvc.perform(get("/pautas/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").value("Pauta nao encontrada"));
    }

    @Test
    @DisplayName("POST /pautas com Content-Type nao suportado retorna 415 com ErroResponse")
    void criar_comContentTypeNaoSuportado_retorna415() throws Exception {
        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("titulo=qualquer"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.mensagem").value("content-type nao suportado"));
    }

    @Test
    @DisplayName("DELETE /pautas/{id} (metodo nao suportado) retorna 405 com ErroResponse")
    void deletarPauta_metodoNaoSuportado_retorna405() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/pautas/{id}", 1))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.mensagem").value("metodo nao suportado"));
    }

    private record PautaRequestTeste(String titulo, String descricao) {
    }
}

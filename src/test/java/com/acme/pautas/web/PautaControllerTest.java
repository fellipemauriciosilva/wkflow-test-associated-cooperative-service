package com.acme.pautas.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PautaControllerTest {

    private static final Set<String> CHAVES_ERRO = Set.of("status", "erro", "mensagem", "path");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void cadastraPautaComTituloERetornaId() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Aprovar contas\", \"descricao\": \"Contas do exercício\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.titulo").value("Aprovar contas"))
                .andExpect(jsonPath("$.descricao").value("Contas do exercício"));
    }

    @Test
    void cadastraPautaSemDescricaoRetornaDescricaoNula() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Sem descrição\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.titulo").value("Sem descrição"))
                .andExpect(jsonPath("$.descricao").value(nullValue()));
    }

    @Test
    void consultaPautaExistente() throws Exception {
        MvcResult criada = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Eleger diretoria\", \"descricao\": \"Chapa única\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long id = mapper.readTree(criada.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Eleger diretoria"))
                .andExpect(jsonPath("$.descricao").value("Chapa única"));
    }

    @Test
    void consultaPautaInexistenteRetorna404NoFormatoPadrao() throws Exception {
        mvc.perform(get("/pautas/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensagem").value(containsString("não encontrada")))
                .andExpect(jsonPath("$.path").value("/pautas/" + Long.MAX_VALUE));
    }

    @Test
    void cadastroSemTituloRetorna400() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\": \"sem titulo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").value(containsString("titulo")))
                .andExpect(jsonPath("$.path").value("/pautas"));
    }

    @Test
    void cadastroComTituloEmBrancoRetorna400() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").value(containsString("titulo")));
    }

    @Test
    void cadastroComTituloAcimaDe255CaracteresRetorna400() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"" + "a".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").value(containsString("titulo")));
    }

    @Test
    void cadastroComDescricaoAcimaDe2000CaracteresRetorna400() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"ok\", \"descricao\": \"" + "d".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").value(containsString("descricao")));
    }

    @Test
    void corpoJsonMalformadoRetorna400NoFormatoPadrao() throws Exception {
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/pautas"));
    }

    @Test
    void idNaoNumericoRetorna400NoFormatoPadrao() throws Exception {
        mvc.perform(get("/pautas/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").value(containsString("id")))
                .andExpect(jsonPath("$.path").value("/pautas/abc"));
    }

    @Test
    void metodoNaoPermitidoRetorna405NoFormatoPadrao() throws Exception {
        MvcResult resultado = mvc.perform(put("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"x\"}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/pautas"))
                .andReturn();
        assertEquals(CHAVES_ERRO, chaves(resultado.getResponse().getContentAsString()));
    }

    @Test
    void tipoDeMidiaNaoSuportadoRetorna415NoFormatoPadrao() throws Exception {
        MvcResult resultado = mvc.perform(post("/pautas")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("titulo=x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/pautas"))
                .andReturn();
        assertEquals(CHAVES_ERRO, chaves(resultado.getResponse().getContentAsString()));
    }

    @Test
    void midiaNaoAceitaRetorna406NoFormatoPadrao() throws Exception {
        MvcResult resultado = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_XML)
                        .content("{\"titulo\": \"Aceite xml\"}"))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.status").value(406))
                .andExpect(jsonPath("$.erro").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/pautas"))
                .andReturn();
        assertEquals(CHAVES_ERRO, chaves(resultado.getResponse().getContentAsString()));
    }

    @Test
    void corpoAcimaDoLimiteRetorna413NoFormatoPadrao() throws Exception {
        String corpo = "{\"titulo\": \"grande\", \"descricao\": \"" + "x".repeat(70_000) + "\"}";
        mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.erro").value("PAYLOAD_MUITO_GRANDE"))
                .andExpect(jsonPath("$.path").value("/pautas"));
    }

    @Test
    void erros400e404TemExatamenteAsMesmasChaves() throws Exception {
        MvcResult erro400 = mvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        MvcResult erro404 = mvc.perform(get("/pautas/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andReturn();

        assertEquals(CHAVES_ERRO, chaves(erro400.getResponse().getContentAsString()));
        assertEquals(CHAVES_ERRO, chaves(erro404.getResponse().getContentAsString()));
    }

    private Set<String> chaves(String corpo) throws Exception {
        JsonNode raiz = mapper.readTree(corpo);
        return StreamSupport.stream(raiz.properties().spliterator(), false)
                .map(java.util.Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}

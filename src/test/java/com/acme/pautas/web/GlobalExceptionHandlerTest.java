package com.acme.pautas.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private static final Set<String> CHAVES_ERRO = Set.of("status", "erro", "mensagem", "path");

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void erroInesperadoRetorna500SemExporMensagemInterna() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/pautas/1");

        ResponseEntity<ErroResponse> resposta = handler.inesperado(
                new IllegalStateException("segredo do banco"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resposta.getStatusCode());
        ErroResponse corpo = resposta.getBody();
        assertEquals(500, corpo.status());
        assertEquals("ERRO_INTERNO", corpo.erro());
        assertEquals("/pautas/1", corpo.path());
        assertFalse(corpo.mensagem().contains("segredo"));
    }

    @Test
    void excecaoNaoTratadaNaRotaRetorna500NoFormatoPadrao() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ControllerQueFalha())
                .setControllerAdvice(handler)
                .build();

        MvcResult resultado = mvc.perform(get("/falha"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.erro").value("ERRO_INTERNO"))
                .andExpect(jsonPath("$.mensagem").value("erro interno"))
                .andExpect(jsonPath("$.path").value("/falha"))
                .andReturn();

        String corpo = resultado.getResponse().getContentAsString();
        assertEquals(CHAVES_ERRO, chaves(corpo));
        assertFalse(corpo.contains("segredo"));
    }

    private Set<String> chaves(String corpo) throws Exception {
        JsonNode raiz = new ObjectMapper().readTree(corpo);
        return StreamSupport.stream(raiz.properties().spliterator(), false)
                .map(java.util.Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @RestController
    static class ControllerQueFalha {

        @GetMapping("/falha")
        String falha() {
            throw new IllegalStateException("segredo do banco");
        }
    }
}

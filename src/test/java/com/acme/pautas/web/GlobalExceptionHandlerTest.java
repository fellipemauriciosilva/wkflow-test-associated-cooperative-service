package com.acme.pautas.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

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
}

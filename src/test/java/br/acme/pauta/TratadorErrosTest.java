package br.acme.pauta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

class TratadorErrosTest {

    private final TratadorErros tratador = new TratadorErros();

    @Test
    void falhaInesperada_retorna500FormatoPadraoComMensagemGenerica() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/pautas");

        var resposta = tratador.inesperado(new IllegalStateException("segredo interno"), request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody()).isEqualTo(
                new ErroPadrao(500, "INTERNAL_ERROR", "Erro interno do servidor", "/pautas"));
    }
}

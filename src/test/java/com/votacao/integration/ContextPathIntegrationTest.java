package com.votacao.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContextPathIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/api";
    }

    @Test
    void criarPauta_sobApiContextPath_retorna201() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{\"titulo\":\"Pauta Integração\",\"descricao\":\"desc\"}", headers);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl() + "/pautas", request, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("id").asLong()).isGreaterThan(0);
    }

    @Test
    void buscarPautaInexistente_sobApiContextPath_retorna404ComPathPrefixado() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(baseUrl() + "/pautas/999999", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("path").asText()).isEqualTo("/api/pautas/999999");
    }

    @Test
    void criarPautaComTituloVazio_sobApiContextPath_retorna400ComPathPrefixado() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{\"titulo\":\"\",\"descricao\":\"x\"}", headers);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl() + "/pautas", request, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("path").asText()).isEqualTo("/api/pautas");
    }
}

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
class ResultadoIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/api";
    }

    @Test
    void fluxoCompleto_criarPautaAbrirSessaoVotarEConsultarResultado_retornaContagensExatas() {
        Long pautaId = criarPauta();
        abrirSessao(pautaId);

        votar(pautaId, 1L, "SIM");
        votar(pautaId, 2L, "SIM");
        votar(pautaId, 3L, "SIM");
        votar(pautaId, 4L, "NAO");
        votar(pautaId, 5L, "NAO");

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                baseUrl() + "/pautas/" + pautaId + "/resultado", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body.get("votos_sim").asInt()).isEqualTo(3);
        assertThat(body.get("votos_nao").asInt()).isEqualTo(2);
        assertThat(body.get("total_votos").asInt()).isEqualTo(5);
        assertThat(body.get("status_sessao").asText()).isEqualTo("ABERTA");
        assertThat(body.get("timestamp").asText()).isNotBlank();
    }

    private Long criarPauta() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(
                "{\"titulo\":\"Pauta Resultado Integração\",\"descricao\":\"desc\"}", headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl() + "/pautas", request, JsonNode.class);
        return response.getBody().get("id").asLong();
    }

    private void abrirSessao(Long pautaId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{\"duracao_minutos\":30}", headers);
        restTemplate.postForEntity(baseUrl() + "/pautas/" + pautaId + "/sessao", request, JsonNode.class);
    }

    private void votar(Long pautaId, long idAssociado, String voto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(
                "{\"id_associado\":" + idAssociado + ",\"voto\":\"" + voto + "\"}", headers);
        restTemplate.postForEntity(baseUrl() + "/pautas/" + pautaId + "/votos", request, JsonNode.class);
    }
}

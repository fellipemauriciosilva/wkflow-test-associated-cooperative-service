package com.votacao.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
class SessaoConcorrenciaIntegrationTest {

    private static final int REQUISICOES_CONCORRENTES = 10;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/api";
    }

    @Test
    void aberturasConcorrentesNaMesmaPauta_exatamenteUmaRetorna201_demaisRetornam422() throws Exception {
        Long pautaId = criarPauta();

        ExecutorService executor = Executors.newFixedThreadPool(REQUISICOES_CONCORRENTES);
        List<Callable<HttpStatus>> tarefas = java.util.stream.Stream
                .generate(() -> (Callable<HttpStatus>) () -> abrirSessao(pautaId))
                .limit(REQUISICOES_CONCORRENTES)
                .collect(Collectors.toList());

        List<Future<HttpStatus>> resultados = executor.invokeAll(tarefas);
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        List<HttpStatus> status = resultados.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

        long criadas = status.stream().filter(s -> s == HttpStatus.CREATED).count();
        long rejeitadas = status.stream().filter(s -> s == HttpStatus.UNPROCESSABLE_ENTITY).count();

        assertThat(criadas)
                .withFailMessage("status obtidos: %s", status)
                .isEqualTo(1);
        assertThat(rejeitadas).isEqualTo(REQUISICOES_CONCORRENTES - 1);
    }

    private Long criarPauta() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{\"titulo\":\"Pauta Concorrencia\",\"descricao\":\"desc\"}", headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl() + "/pautas", request, JsonNode.class);
        return response.getBody().get("id").asLong();
    }

    private HttpStatus abrirSessao(Long pautaId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{}", headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                baseUrl() + "/pautas/" + pautaId + "/sessao", request, JsonNode.class);
        return HttpStatus.valueOf(response.getStatusCode().value());
    }
}

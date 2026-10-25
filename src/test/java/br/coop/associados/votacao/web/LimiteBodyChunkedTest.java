package br.coop.associados.votacao.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LimiteBodyChunkedTest {

    @LocalServerPort
    private int porta;

    @Test
    void corpoChunkedAcimaDoLimiteRetorna413SemContentLength() throws IOException {
        String corpo = "{\"titulo\":\"x\",\"descricao\":\"" + "d".repeat(20 * 1024) + "\"}";

        HttpURLConnection conexao = (HttpURLConnection) URI.create("http://localhost:" + porta + "/pautas")
                .toURL().openConnection();
        conexao.setRequestMethod("POST");
        conexao.setDoOutput(true);
        conexao.setChunkedStreamingMode(1024);
        conexao.setRequestProperty("Content-Type", "application/json");
        try (OutputStream saida = conexao.getOutputStream()) {
            saida.write(corpo.getBytes(StandardCharsets.UTF_8));
        }

        assertThat(conexao.getResponseCode()).isEqualTo(413);
        assertThat(conexao.getHeaderField("Content-Length")).isNull();
        String resposta;
        try (var entrada = conexao.getErrorStream()) {
            resposta = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(resposta).contains("\"status\":413");
        assertThat(resposta).contains("\"message\":\"" + LimiteBodyFilter.MENSAGEM_LIMITE + "\"");
        assertThat(resposta).contains("\"path\":\"/pautas\"");
        conexao.disconnect();
    }
}

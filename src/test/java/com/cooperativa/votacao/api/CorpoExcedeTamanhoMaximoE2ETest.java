package com.cooperativa.votacao.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Sobe um Tomcat embarcado real (nao MockMvc) e fala HTTP/1.1 chunked via socket crudo,
 * porque MockMvc sempre deriva Content-Length do array de bytes do payload — nunca produz
 * uma requisicao chunked de fato. Esse e o cenario exato do bypass H1: sem este teste, a
 * correcao do catch em DispatcherServlet (CorpoExcedeTamanhoMaximoHandler) nunca e provada
 * ponta a ponta, so na unidade isolada do filtro.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CorpoExcedeTamanhoMaximoE2ETest {

    @LocalServerPort
    private int porta;

    @Test
    @DisplayName("POST chunked com body acima do limite retorna 413 JSON de ponta a ponta, sem Content-Length")
    void postChunkedAcimaDoLimite_retorna413() throws Exception {
        // String JSON valida (nao "aaaa..." cru): precisa ser sintaticamente plausivel ate
        // o limite ser estourado, senao o parser do Jackson falha rapido por token
        // invalido antes mesmo de consumir bytes suficientes pra provar o limite.
        var descricaoGigante = "a".repeat((int) TamanhoMaximoBodyFilter.TAMANHO_MAXIMO_BYTES + 1000);
        var corpoAcimaDoLimite = ("{\"titulo\": \"x\", \"descricao\": \"" + descricaoGigante + "\"}")
                .getBytes(StandardCharsets.UTF_8);

        var resposta = enviarRequisicaoChunked(corpoAcimaDoLimite);

        assertThat(resposta).contains("413");
        assertThat(resposta).contains("\"status\":413");
        assertThat(resposta).contains("corpo da requisicao excede o tamanho maximo permitido");
    }

    @Test
    @DisplayName("POST chunked com JSON malformado (nao relacionado ao limite) continua retornando 400, nao 413")
    void postChunkedComJsonMalformado_retorna400_naoConfundeComLimite() throws Exception {
        var jsonMalformadoPequeno = "{invalido".getBytes(StandardCharsets.UTF_8);

        var resposta = enviarRequisicaoChunked(jsonMalformadoPequeno);

        assertThat(resposta).contains("400");
        assertThat(resposta).contains("corpo da requisicao invalido ou ausente");
        assertThat(resposta).doesNotContain("413");
    }

    @Test
    @DisplayName("POST chunked com body dentro do limite cria a pauta normalmente (201)")
    void postChunkedDentroDoLimite_retorna201() throws Exception {
        var payload = "{\"titulo\": \"Pauta via chunked\", \"descricao\": \"corpo pequeno\"}"
                .getBytes(StandardCharsets.UTF_8);

        var resposta = enviarRequisicaoChunked(payload);

        assertThat(resposta).contains("201");
        assertThat(resposta).contains("Pauta via chunked");
    }

    private String enviarRequisicaoChunked(byte[] corpo) throws IOException {
        try (var socket = new Socket("localhost", porta)) {
            socket.setSoTimeout(10_000);
            var out = socket.getOutputStream();
            var requestLine = "POST /pautas HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Type: application/json\r\n"
                    + "Transfer-Encoding: chunked\r\n"
                    + "Connection: close\r\n"
                    + "\r\n";
            out.write(requestLine.getBytes(StandardCharsets.US_ASCII));
            out.flush();

            try {
                escreverChunk(out, corpo);
                out.write("0\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                out.flush();
            } catch (IOException ignorada) {
                // Esperado quando o body excede o limite: o servidor para de ler e pode
                // fechar/resetar a conexao antes do client terminar de escrever o chunk.
                // A resposta 413 pode ja ter sido enviada antes do reset; segue pra leitura.
            }

            return lerResposta(socket);
        }
    }

    private void escreverChunk(OutputStream out, byte[] dados) throws IOException {
        var tamanhoHex = Integer.toHexString(dados.length);
        out.write((tamanhoHex + "\r\n").getBytes(StandardCharsets.US_ASCII));
        out.write(dados);
        out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    private String lerResposta(Socket socket) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        var resposta = new StringBuilder();
        String linha;
        try {
            while ((linha = reader.readLine()) != null) {
                resposta.append(linha).append('\n');
            }
        } catch (IOException ignorada) {
            // Conexao pode ser resetada apos a resposta ser entregue; o que ja foi lido
            // ate aqui e suficiente pras asserções.
        }
        return resposta.toString();
    }
}

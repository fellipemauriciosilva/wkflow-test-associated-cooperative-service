package com.votacao.api.voto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class VotoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long criarPauta(String titulo) throws Exception {
        String body = objectMapper.writeValueAsString(new PautaRequestFixture(titulo, "desc"));
        String response = mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void abrirSessao(Long pautaId, Integer duracaoMinutos) throws Exception {
        String body = duracaoMinutos != null ? "{\"duracao_minutos\":" + duracaoMinutos + "}" : "{}";
        mockMvc.perform(post("/pautas/{id}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void registrarVoto_sessaoAbertaEPayloadValido_retorna201() throws Exception {
        Long pautaId = criarPauta("Pauta Voto A");
        abrirSessao(pautaId, 5);

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_associado\":10,\"voto\":\"SIM\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.voto").value("SIM"))
                .andExpect(jsonPath("$.idAssociado").value(10));
    }

    @Test
    void registrarVoto_segundoVotoDoMesmoAssociado_retorna409() throws Exception {
        Long pautaId = criarPauta("Pauta Voto B");
        abrirSessao(pautaId, 5);

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_associado\":10,\"voto\":\"SIM\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_associado\":10,\"voto\":\"NAO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.path").value("/pautas/" + pautaId + "/votos"));
    }

    @Test
    void registrarVoto_semSessaoAberta_retorna422() throws Exception {
        Long pautaId = criarPauta("Pauta Voto C");

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_associado\":10,\"voto\":\"SIM\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void registrarVoto_pautaInexistente_retorna404() throws Exception {
        mockMvc.perform(post("/pautas/{id}/votos", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_associado\":10,\"voto\":\"SIM\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void registrarVoto_payloadInvalido_retorna400() throws Exception {
        Long pautaId = criarPauta("Pauta Voto D");
        abrirSessao(pautaId, 5);

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_associado\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void registrarVoto_tipoVotoInvalido_retorna400() throws Exception {
        Long pautaId = criarPauta("Pauta Voto E");
        abrirSessao(pautaId, 5);

        mockMvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_associado\":10,\"voto\":\"TALVEZ\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    private record PautaRequestFixture(String titulo, String descricao) {
    }
}

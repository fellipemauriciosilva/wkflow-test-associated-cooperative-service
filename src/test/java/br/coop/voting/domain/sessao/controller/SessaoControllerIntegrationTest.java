package br.coop.voting.domain.sessao.controller;

import br.coop.voting.domain.pauta.entity.Pauta;
import br.coop.voting.domain.pauta.repository.PautaRepository;
import br.coop.voting.domain.sessao.dto.SessaoRequest;
import br.coop.voting.domain.sessao.repository.SessaoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SessaoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PautaRepository pautaRepository;

    @Autowired
    private SessaoRepository sessaoRepository;

    private Long pautaId;

    @BeforeEach
    void setUp() {
        sessaoRepository.deleteAll();
        pautaRepository.deleteAll();

        Pauta pauta = new Pauta();
        pauta.setTitulo("Pauta Teste");
        pauta.setDescricao("Descrição válida e suficiente");

        Pauta saved = pautaRepository.save(pauta);
        pautaId = saved.getId();
    }

    @Test
    void testPostAbrirSessaoRetorna201Created() throws Exception {
        SessaoRequest request = new SessaoRequest(5);

        mockMvc.perform(post("/pautas/{pautaId}/sessao", pautaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").isString())
            .andExpect(jsonPath("$.pauta_id").value(pautaId.intValue()))
            .andExpect(jsonPath("$.duracao_minutos").value(5))
            .andExpect(jsonPath("$.ativa").value(true));
    }

    @Test
    void testPostAbrirSessaoDurationInvalida() throws Exception {
        SessaoRequest request = new SessaoRequest(0);

        mockMvc.perform(post("/pautas/{pautaId}/sessao", pautaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testPostAbrirSessaoPautaNaoEncontrada() throws Exception {
        SessaoRequest request = new SessaoRequest(5);

        mockMvc.perform(post("/pautas/999/sessao")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void testPostAbrirSessaoSegundaSessaoRetorna409Conflict() throws Exception {
        SessaoRequest request = new SessaoRequest(5);

        mockMvc.perform(post("/pautas/{pautaId}/sessao", pautaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/pautas/{pautaId}/sessao", pautaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("SessaoAtivaException"));
    }

    @Test
    void testGetPautaIncluiStatusSessaoAberta() throws Exception {
        SessaoRequest request = new SessaoRequest(5);

        mockMvc.perform(post("/pautas/{pautaId}/sessao", pautaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/pautas/{id}", pautaId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status_sessao").value("SESSAO_ABERTA"))
            .andExpect(jsonPath("$.sessao").exists())
            .andExpect(jsonPath("$.sessao.ativa").value(true));
    }

    @Test
    void testPostAbrirSessaoDurationMax() throws Exception {
        SessaoRequest request = new SessaoRequest(60);

        mockMvc.perform(post("/pautas/{pautaId}/sessao", pautaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.duracao_minutos").value(60));
    }

    @Test
    void testPostAbrirSessaoDurationMaxExcedido() throws Exception {
        SessaoRequest request = new SessaoRequest(61);

        mockMvc.perform(post("/pautas/{pautaId}/sessao", pautaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testLocationHeaderIncluido() throws Exception {
        SessaoRequest request = new SessaoRequest(5);

        mockMvc.perform(post("/pautas/{pautaId}/sessao", pautaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(header().string("Location", containsString("/pautas/" + pautaId + "/sessao/")));
    }

    @Test
    void testAbrirSessaoConcurrentRequests() throws Exception {
        SessaoRequest request1 = new SessaoRequest(5);
        SessaoRequest request2 = new SessaoRequest(3);

        var result1 = mockMvc.perform(post("/pautas/{pautaId}/sessao", pautaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request1)))
            .andExpect(status().isCreated())
            .andReturn();

        var result2 = mockMvc.perform(post("/pautas/{pautaId}/sessao", pautaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request2)))
            .andExpect(status().isConflict())
            .andReturn();

        assert result1.getResponse().getStatus() == 201;
        assert result2.getResponse().getStatus() == 409;
    }
}

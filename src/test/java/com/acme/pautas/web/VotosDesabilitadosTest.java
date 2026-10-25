package com.acme.pautas.web;

import com.acme.pautas.RelogioDeTeste;
import com.acme.pautas.repository.VotoRepository;
import com.acme.pautas.service.PautaService;
import com.acme.pautas.service.SessaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RelogioDeTeste.class)
class VotosDesabilitadosTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PautaService pautas;

    @Autowired
    private SessaoService sessoes;

    @Autowired
    private VotoRepository votos;

    @Test
    void votoRecusadoComServicoIndisponivelQuandoFlagDesligada() throws Exception {
        Long pautaId = pautas.cadastrar("Pauta com votos desligados", null).getId();
        sessoes.abrir(pautaId, 5);

        mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\": \"a1\", \"voto\": \"SIM\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.erro").value("INDISPONIVEL"));

        assertEquals(0, votos.countByPautaId(pautaId));
    }
}

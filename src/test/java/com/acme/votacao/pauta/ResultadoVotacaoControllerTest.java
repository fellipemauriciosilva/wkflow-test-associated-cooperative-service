package com.acme.votacao.pauta;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
class ResultadoVotacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PautaRepository pautaRepository;

    @Autowired
    private SessaoRepository sessaoRepository;

    @Autowired
    private VotoRepository votoRepository;

    @Autowired
    private MutableClock clock;

    private Long pautaId;

    @BeforeEach
    void setUp() {
        clock.definir(Instant.parse("2026-01-01T10:00:00Z"));
        Pauta pauta = pautaRepository.save(new Pauta("Reforma do estatuto", "Discussão sobre regras internas"));
        pautaId = pauta.getId();
    }

    private void abrirSessao(int duracaoMinutos) {
        Instant agora = Instant.parse("2026-01-01T10:00:00Z");
        sessaoRepository.save(new Sessao(pautaId, agora, agora.plusSeconds(60L * duracaoMinutos), duracaoMinutos));
    }

    @Test
    @DisplayName("AC-3.5: resultado com sessão aberta retorna totais corretos e status ABERTA")
    void resultado_sessaoAberta_retornaTotaisEStatusAberta() throws Exception {
        abrirSessao(60);
        votoRepository.save(new Voto(pautaId, "associado-1", VotoValor.SIM, Instant.parse("2026-01-01T10:00:00Z")));
        votoRepository.save(new Voto(pautaId, "associado-2", VotoValor.SIM, Instant.parse("2026-01-01T10:00:00Z")));
        votoRepository.save(new Voto(pautaId, "associado-3", VotoValor.NAO, Instant.parse("2026-01-01T10:00:00Z")));

        mockMvc.perform(get("/pautas/{id}/resultado", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pautaId").value(pautaId))
                .andExpect(jsonPath("$.totalSim").value(2))
                .andExpect(jsonPath("$.totalNao").value(1))
                .andExpect(jsonPath("$.status").value("ABERTA"));
    }

    @Test
    @DisplayName("AC-3.5: resultado com sessão encerrada mantém as mesmas contagens e status ENCERRADA")
    void resultado_sessaoEncerrada_mantemContagensComStatusEncerrada() throws Exception {
        abrirSessao(60);
        votoRepository.save(new Voto(pautaId, "associado-1", VotoValor.SIM, Instant.parse("2026-01-01T10:00:00Z")));
        votoRepository.save(new Voto(pautaId, "associado-2", VotoValor.SIM, Instant.parse("2026-01-01T10:00:00Z")));
        votoRepository.save(new Voto(pautaId, "associado-3", VotoValor.NAO, Instant.parse("2026-01-01T10:00:00Z")));

        clock.definir(Instant.parse("2026-01-01T11:30:00Z"));

        mockMvc.perform(get("/pautas/{id}/resultado", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSim").value(2))
                .andExpect(jsonPath("$.totalNao").value(1))
                .andExpect(jsonPath("$.status").value("ENCERRADA"));
    }

    @Test
    @DisplayName("AC-3.6: GET /pautas/{id}/resultado com pauta inexistente retorna 404")
    void resultado_pautaInexistente_retorna404() throws Exception {
        mockMvc.perform(get("/pautas/{id}/resultado", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("AC-3.7: resultado de pauta sem sessão retorna 200 com totais zero e status NAO_INICIADA")
    void resultado_pautaSemSessao_retornaNaoIniciadaComTotaisZero() throws Exception {
        mockMvc.perform(get("/pautas/{id}/resultado", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSim").value(0))
                .andExpect(jsonPath("$.totalNao").value(0))
                .andExpect(jsonPath("$.status").value("NAO_INICIADA"));
    }
}

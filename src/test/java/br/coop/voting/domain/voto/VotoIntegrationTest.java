package br.coop.voting.domain.voto;

import br.coop.voting.domain.pauta.entity.Pauta;
import br.coop.voting.domain.pauta.repository.PautaRepository;
import br.coop.voting.domain.sessao.entity.Sessao;
import br.coop.voting.domain.sessao.repository.SessaoRepository;
import br.coop.voting.domain.voto.entity.Voto;
import br.coop.voting.domain.voto.entity.VotoEnum;
import br.coop.voting.domain.voto.repository.VotoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class VotoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PautaRepository pautaRepository;

    @Autowired
    private SessaoRepository sessaoRepository;

    @Autowired
    private VotoRepository votoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Pauta pauta;
    private Sessao sessao;

    @BeforeEach
    void setUp() {
        pauta = new Pauta();
        pauta.setTitulo("Pauta de Teste");
        pauta.setDescricao("Descrição da pauta de teste para votação");
        pauta = pautaRepository.save(pauta);

        sessao = new Sessao();
        sessao.setPautaId(pauta.getId());
        sessao.setDurationMinutes(5);
        sessao.setDataAbertura(LocalDateTime.now());
        sessao.setAtiva(true);
        sessao = sessaoRepository.save(sessao);
    }

    @Test
    void testFluxoCompletoVotacao() throws Exception {
        // 1. Registrar primeiro voto SIM
        String requestSIM = "{\"idAssociado\":\"A123\",\"voto\":\"SIM\"}";
        mockMvc.perform(post("/pautas/{id}/votos", pauta.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestSIM))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.voto").value("SIM"));

        // 2. Registrar segundo voto SIM (associado diferente)
        String requestSIM2 = "{\"idAssociado\":\"A456\",\"voto\":\"SIM\"}";
        mockMvc.perform(post("/pautas/{id}/votos", pauta.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestSIM2))
            .andExpect(status().isCreated());

        // 3. Registrar voto NAO
        String requestNAO = "{\"idAssociado\":\"A789\",\"voto\":\"NAO\"}";
        mockMvc.perform(post("/pautas/{id}/votos", pauta.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestNAO))
            .andExpect(status().isCreated());

        // 4. Consultar resultado
        mockMvc.perform(get("/pautas/{id}/resultado", pauta.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sim").value(2L))
            .andExpect(jsonPath("$.nao").value(1L));

        // 5. Tentar registrar voto duplicado (mesmo associado)
        mockMvc.perform(post("/pautas/{id}/votos", pauta.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestSIM))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("VotoDuplicadoException"));

        // 6. Verificar que resultado não mudou após tentativa de duplicação
        mockMvc.perform(get("/pautas/{id}/resultado", pauta.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sim").value(2L))
            .andExpect(jsonPath("$.nao").value(1L));
    }

    @Test
    void testVotoEmPautaSemSessao() throws Exception {
        Pauta pautaSemSessao = new Pauta();
        pautaSemSessao.setTitulo("Pauta Sem Sessão");
        pautaSemSessao.setDescricao("Pauta sem sessão aberta para votação");
        pautaSemSessao = pautaRepository.save(pautaSemSessao);

        String request = "{\"idAssociado\":\"A123\",\"voto\":\"SIM\"}";
        mockMvc.perform(post("/pautas/{id}/votos", pautaSemSessao.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VotacaoFechadaException"));
    }

    @Test
    void testVotoEmPautaFechada() throws Exception {
        // Fechar sessão
        sessao.setAtiva(false);
        sessao.setDataFechamento(LocalDateTime.now());
        sessaoRepository.save(sessao);

        String request = "{\"idAssociado\":\"A123\",\"voto\":\"SIM\"}";
        mockMvc.perform(post("/pautas/{id}/votos", pauta.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VotacaoFechadaException"));
    }

    @Test
    void testVotoComValoresCase() throws Exception {
        // Teste com lowercase
        String requestLowercase = "{\"idAssociado\":\"A123\",\"voto\":\"sim\"}";
        mockMvc.perform(post("/pautas/{id}/votos", pauta.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestLowercase))
            .andExpect(status().isCreated());

        // Verificar que foi normalizado para SIM
        mockMvc.perform(get("/pautas/{id}/resultado", pauta.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sim").value(1L));
    }

    @Test
    void testMesmoAssociadoVotasEmPautasDiferentes() throws Exception {
        // Criar segunda pauta com sessão
        Pauta pauta2 = new Pauta();
        pauta2.setTitulo("Pauta 2");
        pauta2.setDescricao("Segunda pauta para teste");
        pauta2 = pautaRepository.save(pauta2);

        Sessao sessao2 = new Sessao();
        sessao2.setPautaId(pauta2.getId());
        sessao2.setDurationMinutes(5);
        sessao2.setDataAbertura(LocalDateTime.now());
        sessao2.setAtiva(true);
        sessao2 = sessaoRepository.save(sessao2);

        String request = "{\"idAssociado\":\"A123\",\"voto\":\"SIM\"}";

        // Votar na pauta 1
        mockMvc.perform(post("/pautas/{id}/votos", pauta.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
            .andExpect(status().isCreated());

        // Votar na pauta 2 (mesmo associado)
        mockMvc.perform(post("/pautas/{id}/votos", pauta2.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
            .andExpect(status().isCreated());

        // Verificar resultados em ambas as pautas
        mockMvc.perform(get("/pautas/{id}/resultado", pauta.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sim").value(1L));

        mockMvc.perform(get("/pautas/{id}/resultado", pauta2.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sim").value(1L));
    }

    @Test
    void testResultadoPersistePosFecharSessao() throws Exception {
        // Registrar votos
        String requestSIM = "{\"idAssociado\":\"A123\",\"voto\":\"SIM\"}";
        String requestNAO = "{\"idAssociado\":\"A456\",\"voto\":\"NAO\"}";

        mockMvc.perform(post("/pautas/{id}/votos", pauta.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestSIM))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/pautas/{id}/votos", pauta.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestNAO))
            .andExpect(status().isCreated());

        // Fechar sessão
        sessao.setAtiva(false);
        sessao.setDataFechamento(LocalDateTime.now());
        sessaoRepository.save(sessao);

        // Consultar resultado após fechar
        mockMvc.perform(get("/pautas/{id}/resultado", pauta.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sim").value(1L))
            .andExpect(jsonPath("$.nao").value(1L));
    }

    @Test
    void testPautaInexistente() throws Exception {
        Long pautaInexistente = 99999L;
        String request = "{\"idAssociado\":\"A123\",\"voto\":\"SIM\"}";

        mockMvc.perform(post("/pautas/{id}/votos", pautaInexistente)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/pautas/{id}/resultado", pautaInexistente))
            .andExpect(status().isNotFound());
    }

    @Test
    void testRegistrarVotoComAssociadosMultiplos() throws Exception {
        for (int i = 0; i < 5; i++) {
            String associado = "ASSOC_" + i;
            String request = "{\"idAssociado\":\"" + associado + "\",\"voto\":\"SIM\"}";

            mockMvc.perform(post("/pautas/{id}/votos", pauta.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/pautas/{id}/resultado", pauta.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sim").value(5L))
            .andExpect(jsonPath("$.nao").value(0L));
    }
}

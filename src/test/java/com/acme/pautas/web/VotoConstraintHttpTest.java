package com.acme.pautas.web;

import com.acme.pautas.RelogioDeTeste;
import com.acme.pautas.domain.OpcaoVoto;
import com.acme.pautas.domain.Voto;
import com.acme.pautas.repository.VotoRepository;
import com.acme.pautas.service.PautaService;
import com.acme.pautas.service.SessaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "pautas.votos.habilitado=true")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import({RelogioDeTeste.class, VotoConstraintHttpTest.RepositorioEspionado.class})
class VotoConstraintHttpTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PautaService pautas;

    @Autowired
    private SessaoService sessoes;

    @Autowired
    private VotoRepository votos;

    @Test
    void duplicidadeQueChegaAoBancoViraConflitoPelaConstraint() throws Exception {
        Long pautaId = pautas.cadastrar("Pauta constraint via HTTP", null).getId();
        sessoes.abrir(pautaId, 5);
        votos.saveAndFlush(new Voto(pautaId, "a1", OpcaoVoto.SIM));
        doReturn(false).when(votos).existsByPautaIdAndAssociadoId(pautaId, "a1");

        mvc.perform(post("/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\": \"a1\", \"voto\": \"NAO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("CONFLITO"));

        assertEquals(1, votos.countByPautaId(pautaId));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RepositorioEspionado {

        @Bean
        static BeanPostProcessor espionaVotoRepository() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String nome) {
                    if (bean instanceof VotoRepository repositorio) {
                        return mock(VotoRepository.class, delegatesTo(repositorio));
                    }
                    return bean;
                }
            };
        }
    }
}

package br.coop.voting.domain.pauta.controller;

import br.coop.voting.domain.pauta.dto.AtualizarPautaRequest;
import br.coop.voting.domain.pauta.dto.PautaResponse;
import br.coop.voting.domain.pauta.service.PautaService;
import br.coop.voting.shared.exception.PautaNaoEncontradaException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PautaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PautaService pautaService;

    private PautaResponse pautaResponse;

    @BeforeEach
    void setUp() {
        pautaResponse = PautaResponse.builder()
            .id(1L)
            .titulo("Pauta Teste")
            .descricao("Descrição da pauta teste")
            .createdAt(LocalDateTime.parse("2025-01-15T10:00:00"))
            .updatedAt(LocalDateTime.parse("2025-01-15T10:00:00"))
            .build();
    }

    @Test
    void testCriarPautaComDadosValidos() throws Exception {
        when(pautaService.criar(any())).thenReturn(pautaResponse);

        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new br.coop.voting.domain.pauta.dto.CriarPautaRequest(
                        "Pauta Teste",
                        "Descrição da pauta teste"
                    )
                )))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(header().string("Location", "/pautas/1"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.titulo").value("Pauta Teste"))
            .andExpect(jsonPath("$.descricao").value("Descrição da pauta teste"));

        verify(pautaService, times(1)).criar(any());
    }

    @Test
    void testCriarPautaComTituloVazio() throws Exception {
        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new br.coop.voting.domain.pauta.dto.CriarPautaRequest(
                        "",
                        "Descrição da pauta teste"
                    )
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validação falhou"));

        verify(pautaService, never()).criar(any());
    }

    @Test
    void testCriarPautaComTituloMuitoCurto() throws Exception {
        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new br.coop.voting.domain.pauta.dto.CriarPautaRequest(
                        "AB",
                        "Descrição válida para teste"
                    )
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validação falhou"));

        verify(pautaService, never()).criar(any());
    }

    @Test
    void testCriarPautaComDescricaoVazia() throws Exception {
        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new br.coop.voting.domain.pauta.dto.CriarPautaRequest(
                        "Pauta Teste",
                        ""
                    )
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validação falhou"));

        verify(pautaService, never()).criar(any());
    }

    @Test
    void testCriarPautaComDescricaoMuitoCurta() throws Exception {
        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new br.coop.voting.domain.pauta.dto.CriarPautaRequest(
                        "Pauta Teste",
                        "12345"
                    )
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validação falhou"));

        verify(pautaService, never()).criar(any());
    }

    @Test
    void testCriarPautaComTituloExcedendo100Caracteres() throws Exception {
        String tituloLongo = "A".repeat(101);
        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new br.coop.voting.domain.pauta.dto.CriarPautaRequest(
                        tituloLongo,
                        "Descrição válida para teste"
                    )
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validação falhou"));

        verify(pautaService, never()).criar(any());
    }

    @Test
    void testCriarPautaComDescricaoExcedendo500Caracteres() throws Exception {
        String descricaoLonga = "A".repeat(501);
        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new br.coop.voting.domain.pauta.dto.CriarPautaRequest(
                        "Pauta Teste",
                        descricaoLonga
                    )
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validação falhou"));

        verify(pautaService, never()).criar(any());
    }

    @Test
    void testObterPautaPorIdExistente() throws Exception {
        when(pautaService.obterPorId(1L)).thenReturn(pautaResponse);

        mockMvc.perform(get("/pautas/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.titulo").value("Pauta Teste"))
            .andExpect(jsonPath("$.descricao").value("Descrição da pauta teste"))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists());

        verify(pautaService, times(1)).obterPorId(1L);
    }

    @Test
    void testObterPautaPorIdInexistente() throws Exception {
        when(pautaService.obterPorId(99L)).thenThrow(
            new PautaNaoEncontradaException("Pauta com id 99 não encontrada")
        );

        mockMvc.perform(get("/pautas/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Pauta não encontrada"));

        verify(pautaService, times(1)).obterPorId(99L);
    }

    @Test
    void testListarTodasAsPautas() throws Exception {
        PautaResponse pauta2 = PautaResponse.builder()
            .id(2L)
            .titulo("Segunda Pauta")
            .descricao("Descrição da segunda pauta")
            .createdAt(LocalDateTime.parse("2025-01-15T11:00:00"))
            .updatedAt(LocalDateTime.parse("2025-01-15T11:00:00"))
            .build();

        when(pautaService.listarTodas()).thenReturn(List.of(pautaResponse, pauta2));

        mockMvc.perform(get("/pautas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[1].id").value(2));

        verify(pautaService, times(1)).listarTodas();
    }

    @Test
    void testBuscarPautasPorTitulo() throws Exception {
        when(pautaService.buscarPorTitulo("Teste")).thenReturn(List.of(pautaResponse));

        mockMvc.perform(get("/pautas?titulo=Teste"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].titulo").value("Pauta Teste"));

        verify(pautaService, times(1)).buscarPorTitulo("Teste");
    }

    @Test
    void testBuscarPautasComTituloEmBranco() throws Exception {
        when(pautaService.listarTodas()).thenReturn(List.of(pautaResponse));

        mockMvc.perform(get("/pautas?titulo="))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(pautaService, times(1)).listarTodas();
        verify(pautaService, never()).buscarPorTitulo("");
    }

    @Test
    void testBuscarPautasComTituloWhitespace() throws Exception {
        when(pautaService.listarTodas()).thenReturn(List.of(pautaResponse));

        mockMvc.perform(get("/pautas?titulo=   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(pautaService, times(1)).listarTodas();
    }

    @Test
    void testAtualizarPautaComDadosValidos() throws Exception {
        when(pautaService.atualizar(eq(1L), any())).thenReturn(pautaResponse);

        mockMvc.perform(put("/pautas/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new br.coop.voting.domain.pauta.dto.AtualizarPautaRequest(
                        "Pauta Teste",
                        "Descrição da pauta teste"
                    )
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.titulo").value("Pauta Teste"));

        verify(pautaService, times(1)).atualizar(eq(1L), any());
    }

    @Test
    void testAtualizarPautaInexistente() throws Exception {
        when(pautaService.atualizar(eq(99L), any())).thenThrow(
            new PautaNaoEncontradaException("Pauta com id 99 não encontrada")
        );

        mockMvc.perform(put("/pautas/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new br.coop.voting.domain.pauta.dto.AtualizarPautaRequest(
                        "Pauta Teste",
                        "Descrição válida"
                    )
                )))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Pauta não encontrada"));

        verify(pautaService, times(1)).atualizar(eq(99L), any());
    }

    @Test
    void testDeletarPautaExistente() throws Exception {
        doNothing().when(pautaService).deletar(1L);

        mockMvc.perform(delete("/pautas/1"))
            .andExpect(status().isNoContent());

        verify(pautaService, times(1)).deletar(1L);
    }

    @Test
    void testDeletarPautaInexistente() throws Exception {
        doThrow(new PautaNaoEncontradaException("Pauta com id 99 não encontrada"))
            .when(pautaService).deletar(99L);

        mockMvc.perform(delete("/pautas/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Pauta não encontrada"));

        verify(pautaService, times(1)).deletar(99L);
    }

    @Test
    void testAtualizarPautaComDescricaoMuitoCurta() throws Exception {
        AtualizarPautaRequest request = new AtualizarPautaRequest(
            "Pauta Atualizada",
            "12345"
        );

        mockMvc.perform(put("/pautas/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validação falhou"));

        verify(pautaService, never()).atualizar(eq(1L), any());
    }

    @Test
    void testAtualizarPautaComDescricaoExcedendo500Caracteres() throws Exception {
        String descricaoLonga = "A".repeat(501);
        AtualizarPautaRequest request = new AtualizarPautaRequest(
            "Pauta Atualizada",
            descricaoLonga
        );

        mockMvc.perform(put("/pautas/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validação falhou"));

        verify(pautaService, never()).atualizar(eq(1L), any());
    }

    @Test
    void testUnexpectedRuntimeException_Returns500() throws Exception {
        when(pautaService.obterPorId(any())).thenThrow(new RuntimeException("Erro inesperado"));

        mockMvc.perform(get("/pautas/1"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Erro interno"))
            .andExpect(jsonPath("$.message").value("Contacte suporte"));

        verify(pautaService, times(1)).obterPorId(1L);
    }
}

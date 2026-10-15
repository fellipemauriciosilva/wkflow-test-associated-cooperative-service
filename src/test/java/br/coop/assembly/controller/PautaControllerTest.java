package br.coop.assembly.controller;

import br.coop.assembly.dto.CreatePautaRequest;
import br.coop.assembly.dto.PautaResponse;
import br.coop.assembly.entity.Pauta;
import br.coop.assembly.exception.PautaNotFoundException;
import br.coop.assembly.exception.PautaValidationException;
import br.coop.assembly.service.PautaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PautaController.class)
@DisplayName("PautaController Tests")
class PautaControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PautaService service;

    private Pauta testPauta;

    @BeforeEach
    void setUp() {
        testPauta = new Pauta();
        testPauta.setId(1L);
        testPauta.setTitle("Test Pauta");
        testPauta.setDescription("Test Description");
        testPauta.setCreatedAt(LocalDateTime.now());
        testPauta.setUpdatedAt(LocalDateTime.now());
        testPauta.setIsDeleted(false);
    }

    @Test
    @DisplayName("Should create pauta and return 201")
    void post_ValidPauta_Returns201WithId() throws Exception {
        CreatePautaRequest request = new CreatePautaRequest("Valid Pauta", "Valid Description");
        when(service.createPauta("Valid Pauta", "Valid Description")).thenReturn(testPauta);

        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Pauta"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(service, times(1)).createPauta("Valid Pauta", "Valid Description");
    }

    @Test
    @DisplayName("Should return 400 when title is missing")
    void post_MissingTitle_Returns400() throws Exception {
        String json = "{\"description\": \"Test Description\"}";

        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());

        verify(service, never()).createPauta(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 400 when title is blank")
    void post_BlankTitle_Returns400() throws Exception {
        CreatePautaRequest request = new CreatePautaRequest("", "Description");

        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());

        verify(service, never()).createPauta(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 400 when title is too short")
    void post_TitleTooShort_Returns400() throws Exception {
        CreatePautaRequest request = new CreatePautaRequest("ab", "Description");

        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(service, never()).createPauta(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 400 when title is too long")
    void post_TitleTooLong_Returns400() throws Exception {
        String longTitle = "a".repeat(256);
        CreatePautaRequest request = new CreatePautaRequest(longTitle, "Description");

        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(service, never()).createPauta(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 400 when description is too long")
    void post_DescriptionTooLong_Returns400() throws Exception {
        String longDescription = "a".repeat(1001);
        CreatePautaRequest request = new CreatePautaRequest("Valid Title", longDescription);

        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(service, never()).createPauta(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 200 when retrieving existing pauta")
    void get_ValidId_Returns200WithPauta() throws Exception {
        when(service.getPautaById(1L)).thenReturn(testPauta);

        mockMvc.perform(get("/pautas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Pauta"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(service, times(1)).getPautaById(1L);
    }

    @Test
    @DisplayName("Should return 404 when pauta not found")
    void get_NonExistentId_Returns404() throws Exception {
        when(service.getPautaById(999L)).thenThrow(new PautaNotFoundException(999L));

        mockMvc.perform(get("/pautas/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAUTA_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Pauta com id 999 não encontrada"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").exists());

        verify(service, times(1)).getPautaById(999L);
    }

    @Test
    @DisplayName("Should return all pautas")
    void get_AllPautas_Returns200WithList() throws Exception {
        Pauta pauta2 = new Pauta();
        pauta2.setId(2L);
        pauta2.setTitle("Second Pauta");
        pauta2.setDescription("Second Description");
        pauta2.setCreatedAt(LocalDateTime.now());
        pauta2.setUpdatedAt(LocalDateTime.now());
        pauta2.setIsDeleted(false);

        List<Pauta> pautas = Arrays.asList(testPauta, pauta2);
        when(service.getAllPautas()).thenReturn(pautas);

        mockMvc.perform(get("/pautas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(service, times(1)).getAllPautas();
    }

    @Test
    @DisplayName("Should return 400 when service throws validation exception")
    void post_ServiceValidationError_Returns400() throws Exception {
        CreatePautaRequest request = new CreatePautaRequest("Valid", "Description");
        when(service.createPauta("Valid", "Description"))
                .thenThrow(new PautaValidationException("Validation failed"));

        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAUTA_VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Should allow null description in request")
    void post_NullDescription_Returns201() throws Exception {
        CreatePautaRequest request = new CreatePautaRequest("Valid Title", null);
        when(service.createPauta("Valid Title", null)).thenReturn(testPauta);

        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(service, times(1)).createPauta("Valid Title", null);
    }
}

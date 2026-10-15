package br.coop.assembly;

import br.coop.assembly.dto.CreatePautaRequest;
import br.coop.assembly.dto.PautaResponse;
import br.coop.assembly.repository.PautaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Pauta Integration Tests")
class PautaIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PautaRepository repository;

    @Test
    @DisplayName("Should create pauta and retrieve it successfully")
    void createPauta_ThenRetrieve_VerifyPersistence() throws Exception {
        // Clear repository
        repository.deleteAll();

        CreatePautaRequest request = new CreatePautaRequest(
                "Pauta Assembleia Q1 2024",
                "Votacao de novos projetos"
        );

        // Create pauta
        MvcResult createResult = mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        PautaResponse createdPauta = objectMapper.readValue(createResponse, PautaResponse.class);

        assertNotNull(createdPauta.getId());
        assertEquals("Pauta Assembleia Q1 2024", createdPauta.getTitle());
        assertEquals("Votacao de novos projetos", createdPauta.getDescription());
        assertNotNull(createdPauta.getCreatedAt());
        assertNotNull(createdPauta.getUpdatedAt());

        // Retrieve pauta
        MvcResult retrieveResult = mockMvc.perform(get("/pautas/" + createdPauta.getId()))
                .andExpect(status().isOk())
                .andReturn();

        String retrieveResponse = retrieveResult.getResponse().getContentAsString();
        PautaResponse retrievedPauta = objectMapper.readValue(retrieveResponse, PautaResponse.class);

        assertEquals(createdPauta.getId(), retrievedPauta.getId());
        assertEquals(createdPauta.getTitle(), retrievedPauta.getTitle());
        assertEquals(createdPauta.getDescription(), retrievedPauta.getDescription());
        assertEquals(createdPauta.getCreatedAt(), retrievedPauta.getCreatedAt());
        assertEquals(createdPauta.getUpdatedAt(), retrievedPauta.getUpdatedAt());
    }

    @Test
    @DisplayName("Should create and retrieve multiple distinct pautas")
    void multiplePautas_CreateAndRetrieveMultiple() throws Exception {
        // Clear repository
        repository.deleteAll();

        CreatePautaRequest request1 = new CreatePautaRequest("Pauta 1", "Description 1");
        CreatePautaRequest request2 = new CreatePautaRequest("Pauta 2", "Description 2");
        CreatePautaRequest request3 = new CreatePautaRequest("Pauta 3", "Description 3");

        // Create three pautas
        MvcResult result1 = mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult result2 = mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult result3 = mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isCreated())
                .andReturn();

        PautaResponse pauta1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                PautaResponse.class
        );
        PautaResponse pauta2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                PautaResponse.class
        );
        PautaResponse pauta3 = objectMapper.readValue(
                result3.getResponse().getContentAsString(),
                PautaResponse.class
        );

        // Verify they have distinct IDs
        assertNotEquals(pauta1.getId(), pauta2.getId());
        assertNotEquals(pauta2.getId(), pauta3.getId());
        assertNotEquals(pauta1.getId(), pauta3.getId());

        // Retrieve each pauta
        mockMvc.perform(get("/pautas/" + pauta1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Pauta 1"));

        mockMvc.perform(get("/pautas/" + pauta2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Pauta 2"));

        mockMvc.perform(get("/pautas/" + pauta3.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Pauta 3"));
    }

    @Test
    @DisplayName("Should return 404 for non-existent pauta")
    void get_NonExistentPauta_Returns404() throws Exception {
        mockMvc.perform(get("/pautas/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAUTA_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should validate title is required on creation")
    void post_MissingTitle_Returns400() throws Exception {
        String json = "{\"description\": \"Test\"}";

        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Should validate title length on creation")
    void post_InvalidTitleLength_Returns400() throws Exception {
        CreatePautaRequest request = new CreatePautaRequest("ab", "Description");

        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Should validate description length on creation")
    void post_InvalidDescriptionLength_Returns400() throws Exception {
        String longDescription = "a".repeat(1001);
        CreatePautaRequest request = new CreatePautaRequest("Valid Title", longDescription);

        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Should accept null description")
    void post_NullDescription_Returns201() throws Exception {
        repository.deleteAll();

        CreatePautaRequest request = new CreatePautaRequest("Valid Title", null);

        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Valid Title"));
    }

    @Test
    @DisplayName("Should retrieve all pautas")
    void get_AllPautas_ReturnsAllCreatedPautas() throws Exception {
        repository.deleteAll();

        CreatePautaRequest request1 = new CreatePautaRequest("Pauta A", "Description A");
        CreatePautaRequest request2 = new CreatePautaRequest("Pauta B", "Description B");

        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/pautas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}

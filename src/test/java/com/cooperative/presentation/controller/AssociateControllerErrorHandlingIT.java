package com.cooperative.presentation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("Associate Controller Error Handling Tests")
class AssociateControllerErrorHandlingIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("test_db")
        .withUsername("testuser")
        .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    // ========== 400 BAD REQUEST SCENARIOS ==========

    @Test
    @DisplayName("Should return 400 when name is missing")
    void testMissingName() throws Exception {
        String requestBody = """
            {
              "document": "12345678901",
              "email": "test@example.com"
            }
            """;

        mockMvc.perform(post("/api/v1/associates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Should return 400 when document is missing")
    void testMissingDocument() throws Exception {
        String requestBody = """
            {
              "name": "John Doe",
              "email": "test@example.com"
            }
            """;

        mockMvc.perform(post("/api/v1/associates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when name is too short (< 2 chars)")
    void testNameTooShort() throws Exception {
        String requestBody = """
            {
              "name": "A",
              "document": "12345678901"
            }
            """;

        mockMvc.perform(post("/api/v1/associates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when email format is invalid")
    void testInvalidEmailFormat() throws Exception {
        String requestBody = """
            {
              "name": "John Doe",
              "document": "12345678901",
              "email": "invalid-email-format"
            }
            """;

        mockMvc.perform(post("/api/v1/associates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when JSON is malformed")
    void testMalformedJSON() throws Exception {
        String requestBody = "{ invalid json }";

        mockMvc.perform(post("/api/v1/associates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isBadRequest());
    }

    // ========== 404 NOT FOUND SCENARIOS ==========

    @Test
    @DisplayName("Should return 404 when associate ID does not exist")
    void testGetNonExistentAssociate() throws Exception {
        mockMvc.perform(get("/api/v1/associates/{id}", 99999))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Should return 404 error response with ProblemDetail format")
    void testNotFoundErrorFormat() throws Exception {
        mockMvc.perform(get("/api/v1/associates/{id}", 99999))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").exists())
            .andExpect(jsonPath("$.title").exists())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").exists());
    }

    // ========== 409 CONFLICT SCENARIOS ==========

    @Test
    @DisplayName("Should return 409 when duplicate document is provided")
    void testDuplicateDocumentConflict() throws Exception {
        // Create first associate
        String createRequest = """
            {
              "name": "John Doe",
              "document": "12345678901",
              "email": "john@example.com"
            }
            """;

        mockMvc.perform(post("/api/v1/associates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createRequest))
            .andExpect(status().isCreated());

        // Try to create duplicate
        mockMvc.perform(post("/api/v1/associates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createRequest))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Should return 409 error response with ProblemDetail format")
    void testConflictErrorFormat() throws Exception {
        // Create first associate
        String createRequest = """
            {
              "name": "John Doe",
              "document": "12345678901"
            }
            """;

        mockMvc.perform(post("/api/v1/associates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createRequest))
            .andExpect(status().isCreated());

        // Try to create duplicate
        mockMvc.perform(post("/api/v1/associates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createRequest))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").exists())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").exists());
    }

    // ========== RESPONSE FORMAT VALIDATION ==========

    @Test
    @DisplayName("All error responses should have Content-Type application/json")
    void testErrorResponseContentType() throws Exception {
        String requestBody = """
            {
              "name": "A",
              "document": "12345678901"
            }
            """;

        mockMvc.perform(post("/api/v1/associates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should include instance field in error response")
    void testErrorResponseIncludesInstance() throws Exception {
        mockMvc.perform(get("/api/v1/associates/{id}", 99999))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.instance").exists());
    }
}

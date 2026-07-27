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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("Assembly Controller Error Handling Tests")
class AssemblyControllerErrorHandlingIT {

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
    @DisplayName("Should return 400 when title is missing")
    void testMissingTitle() throws Exception {
        String requestBody = """
            {
              "description": "Some description"
            }
            """;

        mockMvc.perform(post("/api/v1/assemblies")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when title is empty")
    void testEmptyTitle() throws Exception {
        String requestBody = """
            {
              "title": "",
              "description": "Some description"
            }
            """;

        mockMvc.perform(post("/api/v1/assemblies")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when title is whitespace")
    void testWhitespaceTitle() throws Exception {
        String requestBody = """
            {
              "title": "   ",
              "description": "Some description"
            }
            """;

        mockMvc.perform(post("/api/v1/assemblies")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when JSON is malformed")
    void testMalformedJSON() throws Exception {
        String requestBody = "{ invalid json }";

        mockMvc.perform(post("/api/v1/assemblies")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isBadRequest());
    }

    // ========== 404 NOT FOUND SCENARIOS ==========

    @Test
    @DisplayName("Should return 404 when assembly ID does not exist")
    void testGetNonExistentAssembly() throws Exception {
        mockMvc.perform(get("/api/v1/assemblies/{id}", 99999))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Should return 404 error response with ProblemDetail format")
    void testNotFoundErrorFormat() throws Exception {
        mockMvc.perform(get("/api/v1/assemblies/{id}", 99999))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").exists())
            .andExpect(jsonPath("$.title").exists())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").exists());
    }

    // ========== RESPONSE FORMAT VALIDATION ==========

    @Test
    @DisplayName("All error responses should have Content-Type application/json")
    void testErrorResponseContentType() throws Exception {
        String requestBody = """
            {
              "title": ""
            }
            """;

        mockMvc.perform(post("/api/v1/assemblies")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should include instance field in error response")
    void testErrorResponseIncludesInstance() throws Exception {
        mockMvc.perform(get("/api/v1/assemblies/{id}", 99999))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.instance").exists());
    }
}

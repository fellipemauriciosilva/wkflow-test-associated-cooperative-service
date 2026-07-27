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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("Input Validation Security Tests")
class InputValidationIT {

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

    // ========== SQL INJECTION PREVENTION ==========

    @Test
    @DisplayName("Should safely handle SQL injection attempts in name field")
    void testSQLInjectionInName() throws Exception {
        String requestBody = """
            {
              "name": "'; DROP TABLE associates; --",
              "document": "12345678901"
            }
            """;

        mockMvc.perform(post("/api/v1/associates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should safely handle SQL injection attempts in document field")
    void testSQLInjectionInDocument() throws Exception {
        String requestBody = """
            {
              "name": "John Doe",
              "document": "' OR '1'='1"
            }
            """;

        mockMvc.perform(post("/api/v1/associates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isCreated());
    }

    // ========== XSS PREVENTION ==========

    @Test
    @DisplayName("Should safely handle XSS attempts in description field")
    void testXSSInDescription() throws Exception {
        String requestBody = """
            {
              "title": "Meeting",
              "description": "<script>alert('XSS')</script>"
            }
            """;

        mockMvc.perform(post("/api/v1/assemblies")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isCreated());
    }

    // ========== UNICODE & SPECIAL CHARACTERS ==========

    @Test
    @DisplayName("Should handle unicode characters in name")
    void testUnicodeInName() throws Exception {
        String requestBody = """
            {
              "name": "José María",
              "document": "12345678901",
              "email": "test@example.com"
            }
            """;

        mockMvc.perform(post("/api/v1/associates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("José María"));
    }

    @Test
    @DisplayName("Should handle emoji and other unicode in title")
    void testEmojiInTitle() throws Exception {
        String requestBody = """
            {
              "title": "Meeting 🚀"
            }
            """;

        mockMvc.perform(post("/api/v1/assemblies")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Meeting 🚀"));
    }

    // ========== CONSTRAINT VALIDATION ==========

    @Test
    @DisplayName("Should reject fields exceeding reasonable length")
    void testExcessiveFieldLength() throws Exception {
        String longString = "A".repeat(1000);
        String requestBody = String.format("""
            {
              "name": "%s",
              "document": "12345678901"
            }
            """, longString);

        mockMvc.perform(post("/api/v1/associates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isBadRequest());
    }

    // ========== VALID SPECIAL CHARACTERS ==========

    @Test
    @DisplayName("Should accept document with special characters")
    void testSpecialCharactersInDocument() throws Exception {
        String requestBody = """
            {
              "name": "John Doe",
              "document": "123.456.789-01"
            }
            """;

        mockMvc.perform(post("/api/v1/associates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should accept title with punctuation")
    void testPunctuationInTitle() throws Exception {
        String requestBody = """
            {
              "title": "Board Meeting - Q1 2025 Review!"
            }
            """;

        mockMvc.perform(post("/api/v1/assemblies")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isCreated());
    }
}

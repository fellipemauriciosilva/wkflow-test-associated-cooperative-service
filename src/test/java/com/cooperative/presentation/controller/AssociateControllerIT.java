package com.cooperative.presentation.controller;

import com.cooperative.application.dto.AssociateResponse;
import com.cooperative.application.dto.CreateAssociateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AssociateControllerIT {

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
    private TestRestTemplate restTemplate;

    @Test
    void testCreateAssociateSuccess() {
        CreateAssociateRequest request = new CreateAssociateRequest(
            "John Doe", "12345678901", "john@example.com");

        ResponseEntity<AssociateResponse> response = restTemplate.postForEntity(
            "/api/v1/associates", request, AssociateResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getHeaders().getLocation());
        assertTrue(response.getHeaders().getLocation().toString()
            .contains("/api/v1/associates/"));
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().id());
        assertEquals("John Doe", response.getBody().name());
    }

    @Test
    void testCreateAssociateDuplicateDocument() {
        CreateAssociateRequest request1 = new CreateAssociateRequest(
            "John Doe", "12345678901", "john@example.com");
        CreateAssociateRequest request2 = new CreateAssociateRequest(
            "Jane Doe", "12345678901", "jane@example.com");

        restTemplate.postForEntity("/api/v1/associates", request1, AssociateResponse.class);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/associates", request2, String.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().contains("already exists"));
    }

    @Test
    void testCreateAssociateValidationError() {
        CreateAssociateRequest request = new CreateAssociateRequest(
            "J", "12345678901", "john@example.com");

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/associates", request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testGetAssociateSuccess() {
        CreateAssociateRequest createRequest = new CreateAssociateRequest(
            "John Doe", "12345678901", "john@example.com");
        ResponseEntity<AssociateResponse> createResponse = restTemplate.postForEntity(
            "/api/v1/associates", createRequest, AssociateResponse.class);
        Long id = createResponse.getBody().id();

        ResponseEntity<AssociateResponse> getResponse = restTemplate.getForEntity(
            "/api/v1/associates/{id}", AssociateResponse.class, id);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("John Doe", getResponse.getBody().name());
    }

    @Test
    void testGetAssociateNotFound() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/v1/associates/99999", String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testListAssociates() {
        CreateAssociateRequest req1 = new CreateAssociateRequest(
            "John Doe", "111", "john@example.com");
        CreateAssociateRequest req2 = new CreateAssociateRequest(
            "Jane Doe", "222", "jane@example.com");
        restTemplate.postForEntity("/api/v1/associates", req1, AssociateResponse.class);
        restTemplate.postForEntity("/api/v1/associates", req2, AssociateResponse.class);

        ResponseEntity<AssociateResponse[]> response = restTemplate.getForEntity(
            "/api/v1/associates", AssociateResponse[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().length >= 2);
    }

    @Test
    void testListAssociatesEmpty() {
        ResponseEntity<AssociateResponse[]> response = restTemplate.getForEntity(
            "/api/v1/associates", AssociateResponse[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}

package com.cooperative.presentation.controller;

import com.cooperative.application.dto.AssemblyResponse;
import com.cooperative.application.dto.CreateAssemblyRequest;
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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AssemblyControllerIT {

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
    void testCreateAssemblySuccess() {
        CreateAssemblyRequest request = new CreateAssemblyRequest(
            "Board Meeting", "Quarterly review", Instant.now());

        ResponseEntity<AssemblyResponse> response = restTemplate.postForEntity(
            "/api/v1/assemblies", request, AssemblyResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getHeaders().getLocation());
        assertTrue(response.getHeaders().getLocation().toString()
            .contains("/api/v1/assemblies/"));
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().id());
        assertEquals("Board Meeting", response.getBody().title());
    }

    @Test
    void testCreateAssemblyValidationError() {
        CreateAssemblyRequest request = new CreateAssemblyRequest(
            "", null, null);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/assemblies", request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testGetAssemblySuccess() {
        CreateAssemblyRequest createRequest = new CreateAssemblyRequest(
            "Board Meeting", "Quarterly review", Instant.now());
        ResponseEntity<AssemblyResponse> createResponse = restTemplate.postForEntity(
            "/api/v1/assemblies", createRequest, AssemblyResponse.class);
        Long id = createResponse.getBody().id();

        ResponseEntity<AssemblyResponse> getResponse = restTemplate.getForEntity(
            "/api/v1/assemblies/{id}", AssemblyResponse.class, id);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("Board Meeting", getResponse.getBody().title());
    }

    @Test
    void testGetAssemblyNotFound() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/v1/assemblies/99999", String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testListAssemblies() {
        CreateAssemblyRequest req1 = new CreateAssemblyRequest(
            "Meeting 1", "Desc 1", Instant.now());
        CreateAssemblyRequest req2 = new CreateAssemblyRequest(
            "Meeting 2", null, null);
        restTemplate.postForEntity("/api/v1/assemblies", req1, AssemblyResponse.class);
        restTemplate.postForEntity("/api/v1/assemblies", req2, AssemblyResponse.class);

        ResponseEntity<AssemblyResponse[]> response = restTemplate.getForEntity(
            "/api/v1/assemblies", AssemblyResponse[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().length >= 2);
    }
}

package com.cooperative.associated.infrastructure.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Associate CRUD REST endpoints.
 * Uses Testcontainers to spin up a real PostgreSQL 16 instance — no mocks/H2.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AssociateControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("associated_cooperative_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String baseUrl() {
        return "http://localhost:" + port + "/associates";
    }

    private HttpEntity<String> jsonEntity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private String uniqueDocument() {
        return "DOC-" + UUID.randomUUID();
    }

    @Test
    void shouldCreateAssociateWithValidData() throws Exception {
        String document = uniqueDocument();
        String body = """
                {"name":"Jane Doe","document":"%s","email":"jane@example.com"}
                """.formatted(document);

        ResponseEntity<String> response =
                restTemplate.postForEntity(baseUrl(), jsonEntity(body), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.get("id").asText()).isNotBlank();
        assertThat(json.get("name").asText()).isEqualTo("Jane Doe");
        assertThat(json.get("document").asText()).isEqualTo(document);
        assertThat(json.get("email").asText()).isEqualTo("jane@example.com");
    }

    @Test
    void shouldCreateAssociateWithoutEmail() throws Exception {
        String document = uniqueDocument();
        String body = """
                {"name":"John Doe","document":"%s"}
                """.formatted(document);

        ResponseEntity<String> response =
                restTemplate.postForEntity(baseUrl(), jsonEntity(body), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void shouldRejectCreationWithoutName() throws Exception {
        String body = """
                {"name":"","document":"%s"}
                """.formatted(uniqueDocument());

        ResponseEntity<String> response =
                restTemplate.postForEntity(baseUrl(), jsonEntity(body), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.has("type")).isTrue();
        assertThat(json.has("title")).isTrue();
        assertThat(json.has("status")).isTrue();
        assertThat(json.has("detail")).isTrue();
        assertThat(json.has("instance")).isTrue();
    }

    @Test
    void shouldRejectDuplicateDocument() throws Exception {
        String document = uniqueDocument();
        String body = """
                {"name":"Alice","document":"%s"}
                """.formatted(document);
        restTemplate.postForEntity(baseUrl(), jsonEntity(body), String.class);

        String secondBody = """
                {"name":"Bob","document":"%s"}
                """.formatted(document);
        ResponseEntity<String> response =
                restTemplate.postForEntity(baseUrl(), jsonEntity(secondBody), String.class);

        assertThat(response.getStatusCode().value()).isIn(400, 409);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.has("type")).isTrue();
        assertThat(json.has("title")).isTrue();
    }

    @Test
    void shouldRejectInvalidEmailFormat() throws Exception {
        String body = """
                {"name":"Carl","document":"%s","email":"invalido"}
                """.formatted(uniqueDocument());

        ResponseEntity<String> response =
                restTemplate.postForEntity(baseUrl(), jsonEntity(body), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldGetAssociateById() throws Exception {
        String body = """
                {"name":"Dana","document":"%s"}
                """.formatted(uniqueDocument());
        ResponseEntity<String> created =
                restTemplate.postForEntity(baseUrl(), jsonEntity(body), String.class);
        String id = objectMapper.readTree(created.getBody()).get("id").asText();

        ResponseEntity<String> response =
                restTemplate.getForEntity(baseUrl() + "/" + id, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.get("id").asText()).isEqualTo(id);
    }

    @Test
    void shouldReturnNotFoundForMissingAssociate() {
        ResponseEntity<String> response =
                restTemplate.getForEntity(baseUrl() + "/" + UUID.randomUUID(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldListAssociates() throws Exception {
        restTemplate.postForEntity(baseUrl(), jsonEntity("""
                {"name":"Eve","document":"%s"}
                """.formatted(uniqueDocument())), String.class);
        restTemplate.postForEntity(baseUrl(), jsonEntity("""
                {"name":"Frank","document":"%s"}
                """.formatted(uniqueDocument())), String.class);

        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.isArray()).isTrue();
        assertThat(json.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldUpdateAssociate() throws Exception {
        String body = """
                {"name":"Grace","document":"%s"}
                """.formatted(uniqueDocument());
        ResponseEntity<String> created =
                restTemplate.postForEntity(baseUrl(), jsonEntity(body), String.class);
        String id = objectMapper.readTree(created.getBody()).get("id").asText();
        String originalDocument = objectMapper.readTree(created.getBody()).get("document").asText();

        String updateBody = """
                {"name":"Grace Updated","document":"%s","email":"grace@example.com"}
                """.formatted(originalDocument);
        ResponseEntity<String> updateResponse = restTemplate.exchange(
                baseUrl() + "/" + id, HttpMethod.PUT, jsonEntity(updateBody), String.class);

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode json = objectMapper.readTree(updateResponse.getBody());
        assertThat(json.get("name").asText()).isEqualTo("Grace Updated");
    }

    @Test
    void shouldRejectUpdateWithEmptyName() throws Exception {
        String body = """
                {"name":"Henry","document":"%s"}
                """.formatted(uniqueDocument());
        ResponseEntity<String> created =
                restTemplate.postForEntity(baseUrl(), jsonEntity(body), String.class);
        String id = objectMapper.readTree(created.getBody()).get("id").asText();
        String document = objectMapper.readTree(created.getBody()).get("document").asText();

        String updateBody = """
                {"name":"","document":"%s"}
                """.formatted(document);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/" + id, HttpMethod.PUT, jsonEntity(updateBody), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectUpdateWithDocumentOfAnotherAssociate() throws Exception {
        String firstDocument = uniqueDocument();
        String secondDocument = uniqueDocument();
        restTemplate.postForEntity(baseUrl(), jsonEntity("""
                {"name":"Ivy","document":"%s"}
                """.formatted(firstDocument)), String.class);
        ResponseEntity<String> secondCreated = restTemplate.postForEntity(baseUrl(), jsonEntity("""
                {"name":"Jack","document":"%s"}
                """.formatted(secondDocument)), String.class);
        String secondId = objectMapper.readTree(secondCreated.getBody()).get("id").asText();

        String updateBody = """
                {"name":"Jack","document":"%s"}
                """.formatted(firstDocument);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/" + secondId, HttpMethod.PUT, jsonEntity(updateBody), String.class);

        assertThat(response.getStatusCode().value()).isIn(400, 409);
    }

    @Test
    void shouldDeleteAssociate() throws Exception {
        String body = """
                {"name":"Karl","document":"%s"}
                """.formatted(uniqueDocument());
        ResponseEntity<String> created =
                restTemplate.postForEntity(baseUrl(), jsonEntity(body), String.class);
        String id = objectMapper.readTree(created.getBody()).get("id").asText();

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                baseUrl() + "/" + id, HttpMethod.DELETE, null, Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> getResponse =
                restTemplate.getForEntity(baseUrl() + "/" + id, String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}

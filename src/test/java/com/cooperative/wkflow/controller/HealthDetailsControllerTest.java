package com.cooperative.wkflow.controller;

import com.cooperative.wkflow.config.ApplicationProperties;
import com.cooperative.wkflow.dto.HealthDetailsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthDetailsController.class)
@DisplayName("HealthDetailsController Tests")
class HealthDetailsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ApplicationProperties applicationProperties;

    @BeforeEach
    void setUp() {
        applicationProperties = new ApplicationProperties();
        applicationProperties.setName("wkflow-test-associated-cooperative-service");
        applicationProperties.setVersion("1.0.0");
    }

    @Test
    @DisplayName("Should return HTTP 200 when getting health details")
    void testGetHealthDetailsReturnsOk() throws Exception {
        mockMvc.perform(get("/health/details")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should return valid JSON structure with name and version fields")
    void testGetHealthDetailsReturnsValidJsonStructure() throws Exception {
        MvcResult result = mockMvc.perform(get("/health/details")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        HealthDetailsResponse response = objectMapper.readValue(responseBody, HealthDetailsResponse.class);

        assertThat(response)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "wkflow-test-associated-cooperative-service")
                .hasFieldOrPropertyWithValue("version", "1.0.0");
    }

    @Test
    @DisplayName("Should not contain null values in response fields")
    void testGetHealthDetailsDoesNotContainNullValues() throws Exception {
        MvcResult result = mockMvc.perform(get("/health/details")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        HealthDetailsResponse response = objectMapper.readValue(responseBody, HealthDetailsResponse.class);

        assertThat(response.getName()).isNotNull().isNotBlank();
        assertThat(response.getVersion()).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("Should have correct response body format")
    void testGetHealthDetailsResponseFormat() throws Exception {
        MvcResult result = mockMvc.perform(get("/health/details")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains("\"name\"").contains("\"version\"");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ApplicationProperties applicationProperties() {
            ApplicationProperties props = new ApplicationProperties();
            props.setName("wkflow-test-associated-cooperative-service");
            props.setVersion("1.0.0");
            return props;
        }
    }
}

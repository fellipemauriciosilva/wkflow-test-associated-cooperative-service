package com.cooperative.wkflow;

import com.cooperative.wkflow.dto.HealthDetailsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Wkflow Application Integration Tests")
class WkflowApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should start the application context successfully")
    void testApplicationContextLoads() {
        assertThat(mockMvc).isNotNull();
    }

    @Test
    @DisplayName("Should have health details endpoint available")
    void testHealthDetailsEndpointExists() throws Exception {
        mockMvc.perform(get("/health/details")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Integration test: health details endpoint returns valid response")
    void testHealthDetailsEndpointIntegration() throws Exception {
        MvcResult result = mockMvc.perform(get("/health/details")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        HealthDetailsResponse response = objectMapper.readValue(responseBody, HealthDetailsResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isNotNull().isNotBlank();
        assertThat(response.getVersion()).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("Integration test: response contains project name from pom.xml")
    void testHealthDetailsResponseContainsProjectName() throws Exception {
        MvcResult result = mockMvc.perform(get("/health/details")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        HealthDetailsResponse response = objectMapper.readValue(responseBody, HealthDetailsResponse.class);

        assertThat(response.getName()).contains("wkflow-test-associated-cooperative-service");
    }

    @Test
    @DisplayName("Integration test: response contains version from pom.xml")
    void testHealthDetailsResponseContainsVersion() throws Exception {
        MvcResult result = mockMvc.perform(get("/health/details")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        HealthDetailsResponse response = objectMapper.readValue(responseBody, HealthDetailsResponse.class);

        assertThat(response.getVersion()).matches("\\d+\\.\\d+\\.\\d+");
    }
}

package com.cooperative.associated.infrastructure.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnHealthDetailsWithNameAndVersion() throws Exception {
        mockMvc.perform(get("/health/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("associated-cooperative-service"))
                .andExpect(jsonPath("$.versao").value("0.1.0"));
    }
}

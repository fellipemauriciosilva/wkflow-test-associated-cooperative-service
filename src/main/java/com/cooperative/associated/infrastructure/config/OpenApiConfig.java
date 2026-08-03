package com.cooperative.associated.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI associatedCooperativeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Associated Cooperative Service API")
                        .description("Cooperative Voting System - Associate & Assembly Foundation (Phase 1: Associate CRUD)")
                        .version("v1")
                        .contact(new Contact().name("Cooperative Voting System")));
    }
}


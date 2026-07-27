package com.cooperative.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 configuration for Springdoc.
 * Enables auto-generated Swagger UI documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cooperativeOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Cooperative Voting System API")
                .description("Phase 1: Associate & Assembly Management")
                .version("v1"))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Local Development")
            ));
    }
}

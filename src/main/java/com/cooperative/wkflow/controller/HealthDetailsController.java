package com.cooperative.wkflow.controller;

import com.cooperative.wkflow.config.ApplicationProperties;
import com.cooperative.wkflow.dto.HealthDetailsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Health check and details endpoints")
public class HealthDetailsController {
    private static final Logger logger = LoggerFactory.getLogger(HealthDetailsController.class);

    private final ApplicationProperties applicationProperties;

    public HealthDetailsController(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @GetMapping("/details")
    @Operation(summary = "Get application health details",
               description = "Returns application name and version from pom.xml configuration")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved health details",
                 content = @Content(schema = @Schema(implementation = HealthDetailsResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error when application metadata is unavailable")
    public ResponseEntity<HealthDetailsResponse> getHealthDetails() {
        try {
            String name = applicationProperties.getName();
            String version = applicationProperties.getVersion();

            if (name == null || name.isBlank() || version == null || version.isBlank()) {
                logger.warn("Application metadata is incomplete: name={}, version={}", name, version);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HealthDetailsResponse("unknown", "unknown"));
            }

            HealthDetailsResponse response = new HealthDetailsResponse(name, version);
            logger.debug("Health details retrieved: {}", response.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving health details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new HealthDetailsResponse("error", "metadata-unavailable"));
        }
    }
}

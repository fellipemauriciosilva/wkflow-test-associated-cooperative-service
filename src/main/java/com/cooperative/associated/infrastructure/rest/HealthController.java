package com.cooperative.associated.infrastructure.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    @Value("${project.name}")
    private String projectName;

    @Value("${project.version}")
    private String projectVersion;

    @GetMapping("/details")
    public ResponseEntity<HealthDetailsResponse> getDetails() {
        return ResponseEntity.ok(new HealthDetailsResponse(projectName, projectVersion));
    }

    public record HealthDetailsResponse(String nome, String versao) {}
}

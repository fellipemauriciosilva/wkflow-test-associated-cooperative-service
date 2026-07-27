package com.cooperative.presentation.controller;

import com.cooperative.application.dto.AssemblyResponse;
import com.cooperative.application.dto.CreateAssemblyRequest;
import com.cooperative.application.usecases.CreateAssemblyUseCase;
import com.cooperative.application.usecases.GetAssemblyUseCase;
import com.cooperative.application.usecases.ListAssembliesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
import java.util.List;

/**
 * REST controller for Assembly endpoints.
 */
@RestController
@RequestMapping("/api/v1/assemblies")
@Validated
@Tag(name = "Assemblies", description = "Assembly management endpoints")
public class AssemblyController {

    private static final Logger logger = LoggerFactory.getLogger(AssemblyController.class);

    private final CreateAssemblyUseCase createAssemblyUseCase;
    private final GetAssemblyUseCase getAssemblyUseCase;
    private final ListAssembliesUseCase listAssembliesUseCase;

    public AssemblyController(
            CreateAssemblyUseCase createAssemblyUseCase,
            GetAssemblyUseCase getAssemblyUseCase,
            ListAssembliesUseCase listAssembliesUseCase) {
        this.createAssemblyUseCase = createAssemblyUseCase;
        this.getAssemblyUseCase = getAssemblyUseCase;
        this.listAssembliesUseCase = listAssembliesUseCase;
    }

    @PostMapping
    @Operation(
        summary = "Create a new assembly",
        description = "Creates a new assembly with validation of title"
    )
    @ApiResponse(responseCode = "201", description = "Assembly created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public ResponseEntity<AssemblyResponse> create(
            @Valid @RequestBody CreateAssemblyRequest request) {
        logger.info("Creating assembly with title: {}", request.title());
        AssemblyResponse response = createAssemblyUseCase.execute(request);
        logger.info("Assembly created with ID: {}", response.id());
        return ResponseEntity
            .created(URI.create("/api/v1/assemblies/" + response.id()))
            .body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get assembly by ID", description = "Retrieves a single assembly by ID")
    @ApiResponse(responseCode = "200", description = "Assembly found")
    @ApiResponse(responseCode = "404", description = "Assembly not found")
    public ResponseEntity<AssemblyResponse> getById(
            @PathVariable @Min(1) Long id) {
        logger.info("Fetching assembly with ID: {}", id);
        AssemblyResponse response = getAssemblyUseCase.execute(id);
        logger.debug("Assembly found: {}", response.title());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List all assemblies", description = "Retrieves all assemblies")
    @ApiResponse(responseCode = "200", description = "List of assemblies")
    public ResponseEntity<List<AssemblyResponse>> list() {
        logger.info("Listing all assemblies");
        List<AssemblyResponse> assemblies = listAssembliesUseCase.execute();
        logger.info("Total assemblies: {}", assemblies.size());
        return ResponseEntity.ok(assemblies);
    }
}

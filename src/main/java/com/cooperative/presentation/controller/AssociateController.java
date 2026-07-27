package com.cooperative.presentation.controller;

import com.cooperative.application.dto.AssociateResponse;
import com.cooperative.application.dto.CreateAssociateRequest;
import com.cooperative.application.usecases.CreateAssociateUseCase;
import com.cooperative.application.usecases.GetAssociateUseCase;
import com.cooperative.application.usecases.ListAssociatesUseCase;
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
 * REST controller for Associate endpoints.
 */
@RestController
@RequestMapping("/api/v1/associates")
@Validated
@Tag(name = "Associates", description = "Associate management endpoints")
public class AssociateController {

    private static final Logger logger = LoggerFactory.getLogger(AssociateController.class);

    private final CreateAssociateUseCase createAssociateUseCase;
    private final GetAssociateUseCase getAssociateUseCase;
    private final ListAssociatesUseCase listAssociatesUseCase;

    public AssociateController(
            CreateAssociateUseCase createAssociateUseCase,
            GetAssociateUseCase getAssociateUseCase,
            ListAssociatesUseCase listAssociatesUseCase) {
        this.createAssociateUseCase = createAssociateUseCase;
        this.getAssociateUseCase = getAssociateUseCase;
        this.listAssociatesUseCase = listAssociatesUseCase;
    }

    @PostMapping
    @Operation(
        summary = "Create a new associate",
        description = "Creates a new associate with validation of name, document uniqueness, and email format"
    )
    @ApiResponse(responseCode = "201", description = "Associate created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "409", description = "Document already exists")
    public ResponseEntity<AssociateResponse> create(
            @Valid @RequestBody CreateAssociateRequest request) {
        logger.info("Creating associate with document: {}", request.document());
        AssociateResponse response = createAssociateUseCase.execute(request);
        logger.info("Associate created with ID: {}", response.id());
        return ResponseEntity
            .created(URI.create("/api/v1/associates/" + response.id()))
            .body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get associate by ID", description = "Retrieves a single associate by ID")
    @ApiResponse(responseCode = "200", description = "Associate found")
    @ApiResponse(responseCode = "404", description = "Associate not found")
    public ResponseEntity<AssociateResponse> getById(
            @PathVariable @Min(1) Long id) {
        logger.info("Fetching associate with ID: {}", id);
        AssociateResponse response = getAssociateUseCase.execute(id);
        logger.debug("Associate found: {}", response.name());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List all associates", description = "Retrieves all associates")
    @ApiResponse(responseCode = "200", description = "List of associates")
    public ResponseEntity<List<AssociateResponse>> list() {
        logger.info("Listing all associates");
        List<AssociateResponse> associates = listAssociatesUseCase.execute();
        logger.info("Total associates: {}", associates.size());
        return ResponseEntity.ok(associates);
    }
}

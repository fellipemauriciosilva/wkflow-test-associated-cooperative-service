package com.cooperative.application.usecases;

import com.cooperative.application.dto.AssociateResponse;
import com.cooperative.application.dto.CreateAssociateRequest;
import com.cooperative.domain.entity.Associate;
import com.cooperative.domain.exception.DuplicateDocumentException;
import com.cooperative.domain.ports.AssociateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use case for creating a new associate.
 */
public class CreateAssociateUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CreateAssociateUseCase.class);

    private final AssociateRepository repository;

    public CreateAssociateUseCase(AssociateRepository repository) {
        this.repository = repository;
    }

    /**
     * Executes associate creation.
     * Validates uniqueness and persists to repository.
     *
     * @param request the create request DTO
     * @return response DTO with created associate
     * @throws DuplicateDocumentException if document already exists
     */
    public AssociateResponse execute(CreateAssociateRequest request) {
        logger.info("Creating associate with document: {}", request.document());
        
        // Validate uniqueness at repository level
        if (repository.findByDocument(request.document()).isPresent()) {
            logger.warn("Duplicate document attempt: {}", request.document());
            throw new DuplicateDocumentException(request.document());
        }

        // Create domain entity (validates name, email format)
        Associate associate = Associate.create(
            request.name(),
            request.document(),
            request.email()
        );

        // Persist
        Associate saved = repository.save(associate);
        logger.info("Associate created with ID: {}", saved.id());

        // Return DTO
        return AssociateResponse.fromDomain(saved);
    }
}

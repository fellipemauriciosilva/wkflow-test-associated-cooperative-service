package com.cooperative.application.usecases;

import com.cooperative.application.dto.AssociateResponse;
import com.cooperative.domain.entity.Associate;
import com.cooperative.domain.exception.EntityNotFoundException;
import com.cooperative.domain.ports.AssociateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use case for retrieving a single associate by ID.
 */
public class GetAssociateUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetAssociateUseCase.class);

    private final AssociateRepository repository;

    public GetAssociateUseCase(AssociateRepository repository) {
        this.repository = repository;
    }

    /**
     * Executes associate retrieval.
     *
     * @param id the associate ID
     * @return response DTO with associate data
     * @throws EntityNotFoundException if associate not found
     */
    public AssociateResponse execute(Long id) {
        logger.debug("Fetching associate with ID: {}", id);
        
        Associate associate = repository.findById(id)
            .orElseThrow(() -> {
                logger.warn("Associate not found with ID: {}", id);
                return new EntityNotFoundException("Associate not found with ID: " + id);
            });

        logger.debug("Associate found: {}", associate.name());
        return AssociateResponse.fromDomain(associate);
    }
}

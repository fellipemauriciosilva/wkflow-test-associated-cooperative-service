package com.cooperative.application.usecases;

import com.cooperative.application.dto.AssemblyResponse;
import com.cooperative.domain.entity.Assembly;
import com.cooperative.domain.exception.EntityNotFoundException;
import com.cooperative.domain.ports.AssemblyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use case for retrieving a single assembly by ID.
 */
public class GetAssemblyUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetAssemblyUseCase.class);

    private final AssemblyRepository repository;

    public GetAssemblyUseCase(AssemblyRepository repository) {
        this.repository = repository;
    }

    /**
     * Executes assembly retrieval.
     *
     * @param id the assembly ID
     * @return response DTO with assembly data
     * @throws EntityNotFoundException if assembly not found
     */
    public AssemblyResponse execute(Long id) {
        logger.debug("Fetching assembly with ID: {}", id);
        
        Assembly assembly = repository.findById(id)
            .orElseThrow(() -> {
                logger.warn("Assembly not found with ID: {}", id);
                return new EntityNotFoundException("Assembly not found with ID: " + id);
            });

        return AssemblyResponse.fromDomain(assembly);
    }
}

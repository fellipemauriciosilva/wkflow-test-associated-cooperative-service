package com.cooperative.application.usecases;

import com.cooperative.application.dto.AssemblyResponse;
import com.cooperative.application.dto.CreateAssemblyRequest;
import com.cooperative.domain.entity.Assembly;
import com.cooperative.domain.ports.AssemblyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use case for creating a new assembly.
 */
public class CreateAssemblyUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CreateAssemblyUseCase.class);

    private final AssemblyRepository repository;

    public CreateAssemblyUseCase(AssemblyRepository repository) {
        this.repository = repository;
    }

    /**
     * Executes assembly creation.
     *
     * @param request the create request DTO
     * @return response DTO with created assembly
     */
    public AssemblyResponse execute(CreateAssemblyRequest request) {
        logger.info("Creating assembly with title: {}", request.title());
        
        Assembly assembly = Assembly.create(
            request.title(),
            request.description(),
            request.scheduledAt()
        );
        
        Assembly saved = repository.save(assembly);
        logger.info("Assembly created with ID: {}", saved.id());
        
        return AssemblyResponse.fromDomain(saved);
    }
}

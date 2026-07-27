package com.cooperative.application.usecases;

import com.cooperative.application.dto.AssemblyResponse;
import com.cooperative.domain.ports.AssemblyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for listing all assemblies.
 */
public class ListAssembliesUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ListAssembliesUseCase.class);

    private final AssemblyRepository repository;

    public ListAssembliesUseCase(AssemblyRepository repository) {
        this.repository = repository;
    }

    /**
     * Executes assembly listing.
     *
     * @return list of all assemblies as response DTOs
     */
    public List<AssemblyResponse> execute() {
        logger.info("Listing all assemblies");
        
        List<AssemblyResponse> assemblies = repository.findAll()
            .stream()
            .map(AssemblyResponse::fromDomain)
            .collect(Collectors.toList());
        
        logger.info("Total assemblies: {}", assemblies.size());
        return assemblies;
    }
}

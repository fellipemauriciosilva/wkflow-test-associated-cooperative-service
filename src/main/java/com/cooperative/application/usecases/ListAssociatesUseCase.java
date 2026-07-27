package com.cooperative.application.usecases;

import com.cooperative.application.dto.AssociateResponse;
import com.cooperative.domain.ports.AssociateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for listing all associates.
 */
public class ListAssociatesUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ListAssociatesUseCase.class);

    private final AssociateRepository repository;

    public ListAssociatesUseCase(AssociateRepository repository) {
        this.repository = repository;
    }

    /**
     * Executes associate listing.
     *
     * @return list of all associates as response DTOs
     */
    public List<AssociateResponse> execute() {
        logger.info("Listing all associates");
        
        List<AssociateResponse> associates = repository.findAll()
            .stream()
            .map(AssociateResponse::fromDomain)
            .collect(Collectors.toList());
        
        logger.info("Total associates: {}", associates.size());
        return associates;
    }
}

package com.cooperative.domain.ports;

import com.cooperative.domain.entity.Associate;
import java.util.List;
import java.util.Optional;

/**
 * Output port (repository interface) for Associate persistence.
 * Implemented by infrastructure layer adapters.
 * No framework dependencies.
 */
public interface AssociateRepository {

    /**
     * Persists an associate.
     *
     * @param associate the associate to persist
     * @return persisted associate with assigned ID
     */
    Associate save(Associate associate);

    /**
     * Retrieves an associate by ID.
     *
     * @param id the associate ID
     * @return Optional containing associate if found, empty otherwise
     */
    Optional<Associate> findById(Long id);

    /**
     * Retrieves an associate by document.
     *
     * @param document the associate document
     * @return Optional containing associate if found, empty otherwise
     */
    Optional<Associate> findByDocument(String document);

    /**
     * Retrieves all associates.
     *
     * @return list of all associates
     */
    List<Associate> findAll();
}

package com.cooperative.infrastructure.adapters.repository;

import com.cooperative.infrastructure.persistence.AssociateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Spring Data JPA repository for Associate.
 * Private implementation detail of JpaAssociateRepository adapter.
 */
@Repository
public interface SpringDataAssociateRepository extends JpaRepository<AssociateEntity, Long> {

    /**
     * Finds an associate by document.
     *
     * @param document the associate document
     * @return Optional containing entity if found
     */
    Optional<AssociateEntity> findByDocument(String document);
}

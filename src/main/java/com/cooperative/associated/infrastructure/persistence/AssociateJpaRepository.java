package com.cooperative.associated.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssociateJpaRepository extends JpaRepository<AssociateJpaEntity, UUID> {

    boolean existsByDocument(String document);

    boolean existsByDocumentAndIdNot(String document, UUID id);
}




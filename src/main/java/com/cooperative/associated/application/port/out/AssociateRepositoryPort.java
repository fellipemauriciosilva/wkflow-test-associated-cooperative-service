package com.cooperative.associated.application.port.out;

import com.cooperative.associated.domain.Associate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssociateRepositoryPort {

    Associate save(Associate associate);

    Optional<Associate> findById(UUID id);

    List<Associate> findAll();

    boolean existsByDocument(String document);

    boolean existsByDocumentAndIdNot(String document, UUID id);

    boolean existsById(UUID id);

    void deleteById(UUID id);
}




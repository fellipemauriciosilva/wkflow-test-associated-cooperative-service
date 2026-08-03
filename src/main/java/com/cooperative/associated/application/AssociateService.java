package com.cooperative.associated.application;

import com.cooperative.associated.application.exception.AssociateNotFoundException;
import com.cooperative.associated.application.exception.DuplicateDocumentException;
import com.cooperative.associated.application.port.out.AssociateRepositoryPort;
import com.cooperative.associated.domain.Associate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AssociateService {

    private final AssociateRepositoryPort repository;

    public AssociateService(AssociateRepositoryPort repository) {
        this.repository = repository;
    }

    @Transactional
    public Associate create(String name, String document, String email) {
        if (repository.existsByDocument(document)) {
            throw new DuplicateDocumentException(document);
        }
        Associate associate = Associate.create(name, document, email);
        return repository.save(associate);
    }

    @Transactional(readOnly = true)
    public Associate findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new AssociateNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Associate> findAll() {
        return repository.findAll();
    }

    @Transactional
    public Associate update(UUID id, String name, String document, String email) {
        Associate existing = repository.findById(id)
                .orElseThrow(() -> new AssociateNotFoundException(id));
        if (repository.existsByDocumentAndIdNot(document, id)) {
            throw new DuplicateDocumentException(document);
        }
        existing.changeName(name);
        existing.changeDocument(document);
        existing.changeEmail(email);
        return repository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new AssociateNotFoundException(id);
        }
        repository.deleteById(id);
    }
}

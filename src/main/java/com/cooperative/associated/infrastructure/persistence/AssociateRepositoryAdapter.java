package com.cooperative.associated.infrastructure.persistence;

import com.cooperative.associated.application.port.out.AssociateRepositoryPort;
import com.cooperative.associated.domain.Associate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AssociateRepositoryAdapter implements AssociateRepositoryPort {

    private final AssociateJpaRepository jpaRepository;
    private final AssociateMapper mapper;

    public AssociateRepositoryAdapter(AssociateJpaRepository jpaRepository, AssociateMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Associate save(Associate associate) {
        AssociateJpaEntity saved = jpaRepository.save(mapper.toJpaEntity(associate));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Associate> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Associate> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByDocument(String document) {
        return jpaRepository.existsByDocument(document);
    }

    @Override
    public boolean existsByDocumentAndIdNot(String document, UUID id) {
        return jpaRepository.existsByDocumentAndIdNot(document, id);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}


package com.cooperative.infrastructure.adapters.repository;

import com.cooperative.domain.entity.Associate;
import com.cooperative.domain.exception.DuplicateDocumentException;
import com.cooperative.domain.ports.AssociateRepository;
import com.cooperative.infrastructure.persistence.AssociateEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA adapter implementing AssociateRepository port.
 * Translates between domain entities and JPA entities.
 */
@Repository
public class JpaAssociateRepository implements AssociateRepository {

    private static final Logger logger = LoggerFactory.getLogger(JpaAssociateRepository.class);

    private final SpringDataAssociateRepository springDataRepository;

    public JpaAssociateRepository(SpringDataAssociateRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    @Transactional
    public Associate save(Associate associate) {
        try {
            AssociateEntity entity = AssociateEntity.fromDomain(associate);
            AssociateEntity saved = springDataRepository.save(entity);
            logger.info("Associate saved with document: {}", saved.getDocument());
            return saved.toDomain();
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("unique constraint")) {
                logger.warn("Unique constraint violation for document");
                throw new DuplicateDocumentException(associate.document());
            }
            logger.error("Data integrity violation", e);
            throw e;
        }
    }

    @Override
    public Optional<Associate> findById(Long id) {
        logger.debug("Fetching associate with ID: {}", id);
        return springDataRepository.findById(id)
            .map(AssociateEntity::toDomain);
    }

    @Override
    public Optional<Associate> findByDocument(String document) {
        logger.debug("Fetching associate with document: {}", document);
        return springDataRepository.findByDocument(document)
            .map(AssociateEntity::toDomain);
    }

    @Override
    public List<Associate> findAll() {
        logger.info("Fetching all associates");
        return springDataRepository.findAll()
            .stream()
            .map(AssociateEntity::toDomain)
            .collect(Collectors.toList());
    }
}

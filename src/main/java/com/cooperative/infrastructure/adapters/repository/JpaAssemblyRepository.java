package com.cooperative.infrastructure.adapters.repository;

import com.cooperative.domain.entity.Assembly;
import com.cooperative.domain.ports.AssemblyRepository;
import com.cooperative.infrastructure.persistence.AssemblyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA adapter implementing AssemblyRepository port.
 * Translates between domain entities and JPA entities.
 */
@Repository
public class JpaAssemblyRepository implements AssemblyRepository {

    private static final Logger logger = LoggerFactory.getLogger(JpaAssemblyRepository.class);

    private final SpringDataAssemblyRepository springDataRepository;

    public JpaAssemblyRepository(SpringDataAssemblyRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    @Transactional
    public Assembly save(Assembly assembly) {
        AssemblyEntity entity = AssemblyEntity.fromDomain(assembly);
        AssemblyEntity saved = springDataRepository.save(entity);
        logger.info("Assembly saved with ID: {}", saved.getId());
        return saved.toDomain();
    }

    @Override
    public Optional<Assembly> findById(Long id) {
        logger.debug("Fetching assembly with ID: {}", id);
        return springDataRepository.findById(id)
            .map(AssemblyEntity::toDomain);
    }

    @Override
    public List<Assembly> findAll() {
        logger.info("Fetching all assemblies");
        return springDataRepository.findAll()
            .stream()
            .map(AssemblyEntity::toDomain)
            .collect(Collectors.toList());
    }
}

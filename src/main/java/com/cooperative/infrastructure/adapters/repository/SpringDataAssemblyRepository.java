package com.cooperative.infrastructure.adapters.repository;

import com.cooperative.infrastructure.persistence.AssemblyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for Assembly.
 * Private implementation detail of JpaAssemblyRepository adapter.
 */
@Repository
public interface SpringDataAssemblyRepository extends JpaRepository<AssemblyEntity, Long> {
}

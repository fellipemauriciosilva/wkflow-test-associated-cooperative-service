package com.cooperative.domain.ports;

import com.cooperative.domain.entity.Assembly;
import java.util.List;
import java.util.Optional;

/**
 * Output port (repository interface) for Assembly persistence.
 * Implemented by infrastructure layer adapters.
 * No framework dependencies.
 */
public interface AssemblyRepository {

    /**
     * Persists an assembly.
     *
     * @param assembly the assembly to persist
     * @return persisted assembly with assigned ID
     */
    Assembly save(Assembly assembly);

    /**
     * Retrieves an assembly by ID.
     *
     * @param id the assembly ID
     * @return Optional containing assembly if found, empty otherwise
     */
    Optional<Assembly> findById(Long id);

    /**
     * Retrieves all assemblies.
     *
     * @return list of all assemblies
     */
    List<Assembly> findAll();
}

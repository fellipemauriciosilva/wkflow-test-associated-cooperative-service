package com.example.cooperativevoting.infrastructure.persistence;

import com.example.cooperativevoting.domain.entities.Assembly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AssemblyRepository extends JpaRepository<Assembly, UUID> {
}

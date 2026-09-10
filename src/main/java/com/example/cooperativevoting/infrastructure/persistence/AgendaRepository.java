package com.example.cooperativevoting.infrastructure.persistence;

import com.example.cooperativevoting.domain.entities.Agenda;
import com.example.cooperativevoting.domain.enums.AgendaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgendaRepository extends JpaRepository<Agenda, UUID> {
    List<Agenda> findByAssemblyId(UUID assemblyId);
    List<Agenda> findByStatus(AgendaStatus status);
}

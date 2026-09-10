package com.example.cooperativevoting.infrastructure.persistence;

import com.example.cooperativevoting.domain.entities.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoteRepository extends JpaRepository<Vote, UUID> {
    Optional<Vote> findByAgendaIdAndAssociateId(UUID agendaId, UUID associateId);
    Optional<Vote> findByEventId(String eventId);
}

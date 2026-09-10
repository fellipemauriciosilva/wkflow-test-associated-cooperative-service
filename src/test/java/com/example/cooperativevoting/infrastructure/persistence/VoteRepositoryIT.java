package com.example.cooperativevoting.infrastructure.persistence;

import com.example.cooperativevoting.domain.entities.*;
import com.example.cooperativevoting.domain.enums.AgendaStatus;
import com.example.cooperativevoting.domain.enums.VoteOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class VoteRepositoryIT {

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private AgendaRepository agendaRepository;

    @Autowired
    private AssemblyRepository assemblyRepository;

    @Autowired
    private AssociateRepository associateRepository;

    private Assembly assembly;
    private Agenda agenda;
    private Associate associate;

    @BeforeEach
    void setUp() {
        assembly = new Assembly(UUID.randomUUID(), "Q1", "Q1 Assembly", LocalDate.now());
        assemblyRepository.save(assembly);

        agenda = new Agenda(UUID.randomUUID(), assembly.getId(), "Agenda 1", "desc",
                AgendaStatus.OPEN, 60);
        agenda.setAssembly(assembly);
        agendaRepository.save(agenda);

        associate = new Associate(UUID.randomUUID(), "John", "doc-vote-001", "john@example.com");
        associateRepository.save(associate);
    }

    @Test
    void testSaveAndFindVote() {
        UUID voteId = UUID.randomUUID();
        Vote vote = new Vote(voteId, agenda.getId(), associate.getId(), VoteOption.YES, "event-001");
        vote.setAgenda(agenda);
        vote.setAssociate(associate);

        Vote saved = voteRepository.save(vote);

        assertEquals(voteId, saved.getId());
        assertEquals(VoteOption.YES, saved.getOption());

        Optional<Vote> found = voteRepository.findById(voteId);
        assertTrue(found.isPresent());
        assertEquals(VoteOption.YES, found.get().getOption());
    }

    @Test
    void testFindByAgendaIdAndAssociateId() {
        Vote vote = new Vote(UUID.randomUUID(), agenda.getId(), associate.getId(),
                VoteOption.YES, "event-002");
        vote.setAgenda(agenda);
        vote.setAssociate(associate);
        voteRepository.save(vote);

        Optional<Vote> found = voteRepository.findByAgendaIdAndAssociateId(agenda.getId(), associate.getId());

        assertTrue(found.isPresent());
        assertEquals(VoteOption.YES, found.get().getOption());
    }

    @Test
    void testFindByEventId() {
        Vote vote = new Vote(UUID.randomUUID(), agenda.getId(), associate.getId(),
                VoteOption.NO, "event-unique-003");
        vote.setAgenda(agenda);
        vote.setAssociate(associate);
        voteRepository.save(vote);

        Optional<Vote> found = voteRepository.findByEventId("event-unique-003");

        assertTrue(found.isPresent());
        assertEquals(VoteOption.NO, found.get().getOption());
    }

    @Test
    void testDuplicateVoteThrowsConstraintViolation() {
        Vote vote1 = new Vote(UUID.randomUUID(), agenda.getId(), associate.getId(),
                VoteOption.YES, "event-004");
        vote1.setAgenda(agenda);
        vote1.setAssociate(associate);
        voteRepository.save(vote1);

        Vote vote2 = new Vote(UUID.randomUUID(), agenda.getId(), associate.getId(),
                VoteOption.NO, "event-005");
        vote2.setAgenda(agenda);
        vote2.setAssociate(associate);

        assertThrows(DataIntegrityViolationException.class, () -> {
            voteRepository.saveAndFlush(vote2);
        });
    }

    @Test
    void testDuplicateEventIdThrowsConstraintViolation() {
        Vote vote1 = new Vote(UUID.randomUUID(), agenda.getId(), associate.getId(),
                VoteOption.YES, "event-duplicate");
        vote1.setAgenda(agenda);
        vote1.setAssociate(associate);
        voteRepository.save(vote1);

        Associate associate2 = new Associate(UUID.randomUUID(), "Jane", "doc-vote-002", "jane@example.com");
        associateRepository.save(associate2);

        Vote vote2 = new Vote(UUID.randomUUID(), agenda.getId(), associate2.getId(),
                VoteOption.NO, "event-duplicate");
        vote2.setAgenda(agenda);
        vote2.setAssociate(associate2);

        assertThrows(DataIntegrityViolationException.class, () -> {
            voteRepository.saveAndFlush(vote2);
        });
    }

    @Test
    void testVoteInvalidAgendaFkThrowsConstraintViolation() {
        Vote vote = new Vote(UUID.randomUUID(), UUID.randomUUID(), associate.getId(),
                VoteOption.YES, "event-006");
        vote.setAssociate(associate);

        assertThrows(DataIntegrityViolationException.class, () -> {
            voteRepository.saveAndFlush(vote);
        });
    }

    @Test
    void testVoteInvalidAssociateFkThrowsConstraintViolation() {
        Vote vote = new Vote(UUID.randomUUID(), agenda.getId(), UUID.randomUUID(),
                VoteOption.YES, "event-007");
        vote.setAgenda(agenda);

        assertThrows(DataIntegrityViolationException.class, () -> {
            voteRepository.saveAndFlush(vote);
        });
    }
}

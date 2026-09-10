package com.example.cooperativevoting.infrastructure.persistence;

import com.example.cooperativevoting.domain.entities.Agenda;
import com.example.cooperativevoting.domain.entities.Assembly;
import com.example.cooperativevoting.domain.enums.AgendaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AgendaRepositoryIT {

    @Autowired
    private AgendaRepository agendaRepository;

    @Autowired
    private AssemblyRepository assemblyRepository;

    private Assembly assembly;

    @BeforeEach
    void setUp() {
        assembly = new Assembly(UUID.randomUUID(), "Q1", "Q1 Assembly", LocalDate.now());
        assemblyRepository.save(assembly);
    }

    @Test
    void testSaveAndFindAgenda() {
        UUID agendaId = UUID.randomUUID();
        Agenda agenda = new Agenda(agendaId, assembly.getId(), "Agenda 1", "desc",
                AgendaStatus.CREATED, 60);
        agenda.setAssembly(assembly);

        Agenda saved = agendaRepository.save(agenda);

        assertEquals(agendaId, saved.getId());
        assertEquals("Agenda 1", saved.getTitle());
        assertEquals(AgendaStatus.CREATED, saved.getStatus());

        Optional<Agenda> found = agendaRepository.findById(agendaId);
        assertTrue(found.isPresent());
        assertEquals("Agenda 1", found.get().getTitle());
    }

    @Test
    void testFindByAssemblyId() {
        Agenda agenda1 = new Agenda(UUID.randomUUID(), assembly.getId(), "Agenda 1", "desc", null, null);
        agenda1.setAssembly(assembly);
        Agenda agenda2 = new Agenda(UUID.randomUUID(), assembly.getId(), "Agenda 2", "desc", null, null);
        agenda2.setAssembly(assembly);

        agendaRepository.save(agenda1);
        agendaRepository.save(agenda2);

        List<Agenda> found = agendaRepository.findByAssemblyId(assembly.getId());

        assertEquals(2, found.size());
    }

    @Test
    void testFindByStatus() {
        Agenda agenda1 = new Agenda(UUID.randomUUID(), assembly.getId(), "Agenda 1", "desc",
                AgendaStatus.CREATED, 60);
        agenda1.setAssembly(assembly);
        Agenda agenda2 = new Agenda(UUID.randomUUID(), assembly.getId(), "Agenda 2", "desc",
                AgendaStatus.OPEN, 60);
        agenda2.setAssembly(assembly);

        agendaRepository.save(agenda1);
        agendaRepository.save(agenda2);

        List<Agenda> created = agendaRepository.findByStatus(AgendaStatus.CREATED);
        List<Agenda> open = agendaRepository.findByStatus(AgendaStatus.OPEN);

        assertEquals(1, created.size());
        assertEquals(1, open.size());
        assertEquals(AgendaStatus.CREATED, created.get(0).getStatus());
        assertEquals(AgendaStatus.OPEN, open.get(0).getStatus());
    }

    @Test
    void testAgendaWithoutAssemblyThrowsConstraintViolation() {
        Agenda agenda = new Agenda(UUID.randomUUID(), UUID.randomUUID(), "Agenda", "desc", null, null);

        assertThrows(DataIntegrityViolationException.class, () -> {
            agendaRepository.saveAndFlush(agenda);
        });
    }

    @Test
    void testAgendaMissingTitleThrowsConstraintViolation() {
        Agenda agenda = new Agenda(UUID.randomUUID(), assembly.getId(), null, "desc", null, null);
        agenda.setAssembly(assembly);

        assertThrows(DataIntegrityViolationException.class, () -> {
            agendaRepository.saveAndFlush(agenda);
        });
    }

    @Test
    void testUpdateAgendaStatus() {
        Agenda agenda = new Agenda(UUID.randomUUID(), assembly.getId(), "Agenda", "desc",
                AgendaStatus.CREATED, 60);
        agenda.setAssembly(assembly);
        agendaRepository.save(agenda);

        agenda.setStatus(AgendaStatus.OPEN);
        agendaRepository.save(agenda);

        Optional<Agenda> found = agendaRepository.findById(agenda.getId());
        assertTrue(found.isPresent());
        assertEquals(AgendaStatus.OPEN, found.get().getStatus());
    }

    @Test
    void testCascadeDeleteAssemblyDeletesAgendas() {
        Agenda agenda = new Agenda(UUID.randomUUID(), assembly.getId(), "Agenda", "desc", null, null);
        agenda.setAssembly(assembly);
        agendaRepository.save(agenda);

        UUID agendaId = agenda.getId();
        assemblyRepository.delete(assembly);

        Optional<Agenda> found = agendaRepository.findById(agendaId);
        assertFalse(found.isPresent());
    }
}

package com.example.cooperativevoting.domain.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AssemblyTest {

    @Test
    void testAssemblyCreation() {
        UUID id = UUID.randomUUID();
        String title = "Q1 Assembly";
        String description = "First quarter assembly";
        LocalDate date = LocalDate.now();

        Assembly assembly = new Assembly(id, title, description, date);

        assertEquals(id, assembly.getId());
        assertEquals(title, assembly.getTitle());
        assertEquals(description, assembly.getDescription());
        assertEquals(date, assembly.getScheduledDate());
        assertNotNull(assembly.getCreatedAt());
        assertNotNull(assembly.getAgendas());
        assertTrue(assembly.getAgendas().isEmpty());
    }

    @Test
    void testAssemblyEquality() {
        UUID id = UUID.randomUUID();
        Assembly assembly1 = new Assembly(id, "Q1", "desc1", LocalDate.now());
        Assembly assembly2 = new Assembly(id, "Q2", "desc2", LocalDate.now());

        assertEquals(assembly1, assembly2);
        assertEquals(assembly1.hashCode(), assembly2.hashCode());
    }

    @Test
    void testAddAgendaToAssembly() {
        Assembly assembly = new Assembly(UUID.randomUUID(), "Q1", "desc", LocalDate.now());
        Agenda agenda = new Agenda(UUID.randomUUID(), UUID.randomUUID(), "Agenda 1", "desc", null, null);

        assembly.addAgenda(agenda);

        assertEquals(1, assembly.getAgendas().size());
        assertEquals(assembly, agenda.getAssembly());
    }
}

package com.example.cooperativevoting.domain.entities;

import com.example.cooperativevoting.domain.enums.AgendaStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AgendaTest {

    @Test
    void testAgendaCreation() {
        UUID id = UUID.randomUUID();
        UUID assemblyId = UUID.randomUUID();
        String title = "Vote on Budget";
        String description = "Annual budget allocation";

        Agenda agenda = new Agenda(id, assemblyId, title, description, null, null);

        assertEquals(id, agenda.getId());
        assertEquals(title, agenda.getTitle());
        assertEquals(description, agenda.getDescription());
        assertEquals(AgendaStatus.CREATED, agenda.getStatus());
        assertEquals(1, agenda.getDurationInMinutes());
        assertNotNull(agenda.getCreatedAt());
        assertNotNull(agenda.getUpdatedAt());
        assertNotNull(agenda.getVotes());
        assertTrue(agenda.getVotes().isEmpty());
    }

    @Test
    void testAgendaWithCustomStatus() {
        Agenda agenda = new Agenda(UUID.randomUUID(), UUID.randomUUID(), "Title", "desc",
                AgendaStatus.OPEN, 30);

        assertEquals(AgendaStatus.OPEN, agenda.getStatus());
        assertEquals(30, agenda.getDurationInMinutes());
    }

    @Test
    void testAgendaEquality() {
        UUID id = UUID.randomUUID();
        Agenda agenda1 = new Agenda(id, UUID.randomUUID(), "Title1", "desc1", null, null);
        Agenda agenda2 = new Agenda(id, UUID.randomUUID(), "Title2", "desc2", null, null);

        assertEquals(agenda1, agenda2);
        assertEquals(agenda1.hashCode(), agenda2.hashCode());
    }

    @Test
    void testAddVoteToAgenda() {
        Agenda agenda = new Agenda(UUID.randomUUID(), UUID.randomUUID(), "Title", "desc", null, null);
        Vote vote = new Vote(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                com.example.cooperativevoting.domain.enums.VoteOption.YES, "event-1");

        agenda.addVote(vote);

        assertEquals(1, agenda.getVotes().size());
        assertEquals(agenda, vote.getAgenda());
    }
}

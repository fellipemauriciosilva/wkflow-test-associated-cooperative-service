package com.example.cooperativevoting.domain.entities;

import com.example.cooperativevoting.domain.enums.VoteOption;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VoteTest {

    @Test
    void testVoteCreation() {
        UUID id = UUID.randomUUID();
        UUID agendaId = UUID.randomUUID();
        UUID associateId = UUID.randomUUID();
        String eventId = "event-123";

        Vote vote = new Vote(id, agendaId, associateId, VoteOption.YES, eventId);

        assertEquals(id, vote.getId());
        assertEquals(VoteOption.YES, vote.getOption());
        assertEquals(eventId, vote.getEventId());
        assertNotNull(vote.getCreatedAt());
    }

    @Test
    void testVoteWithAbstain() {
        Vote vote = new Vote(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                VoteOption.ABSTAIN, "event-456");

        assertEquals(VoteOption.ABSTAIN, vote.getOption());
    }

    @Test
    void testVoteWithNo() {
        Vote vote = new Vote(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                VoteOption.NO, "event-789");

        assertEquals(VoteOption.NO, vote.getOption());
    }

    @Test
    void testVoteEquality() {
        UUID id = UUID.randomUUID();
        Vote vote1 = new Vote(id, UUID.randomUUID(), UUID.randomUUID(), VoteOption.YES, "event-1");
        Vote vote2 = new Vote(id, UUID.randomUUID(), UUID.randomUUID(), VoteOption.NO, "event-2");

        assertEquals(vote1, vote2);
        assertEquals(vote1.hashCode(), vote2.hashCode());
    }

    @Test
    void testVoteFieldUpdate() {
        Vote vote = new Vote(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                VoteOption.YES, "event-1");

        vote.setOption(VoteOption.NO);
        assertEquals(VoteOption.NO, vote.getOption());
    }
}

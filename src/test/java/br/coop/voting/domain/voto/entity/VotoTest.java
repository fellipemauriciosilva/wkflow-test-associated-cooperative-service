package br.coop.voting.domain.voto.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class VotoTest {

    @Test
    void testVotoCreation() {
        Voto voto = new Voto();
        voto.setId(1L);
        voto.setPautaId(10L);
        voto.setSessaoId("sess-1");
        voto.setIdAssociado("A123");
        voto.setVoto(VotoEnum.SIM);

        assertEquals(1L, voto.getId());
        assertEquals(10L, voto.getPautaId());
        assertEquals("sess-1", voto.getSessaoId());
        assertEquals("A123", voto.getIdAssociado());
        assertEquals(VotoEnum.SIM, voto.getVoto());
    }

    @Test
    void testVotoEqualsAndHashCode() {
        Voto voto1 = new Voto();
        voto1.setId(1L);
        voto1.setPautaId(10L);
        voto1.setSessaoId("sess-1");
        voto1.setIdAssociado("A123");
        voto1.setVoto(VotoEnum.SIM);

        Voto voto2 = new Voto();
        voto2.setId(1L);
        voto2.setPautaId(10L);
        voto2.setSessaoId("sess-1");
        voto2.setIdAssociado("A123");
        voto2.setVoto(VotoEnum.SIM);

        assertEquals(voto1, voto2);
        assertEquals(voto1.hashCode(), voto2.hashCode());
    }

    @Test
    void testVotoEnumValues() {
        assertEquals(VotoEnum.SIM, VotoEnum.valueOf("SIM"));
        assertEquals(VotoEnum.NAO, VotoEnum.valueOf("NAO"));
    }
}

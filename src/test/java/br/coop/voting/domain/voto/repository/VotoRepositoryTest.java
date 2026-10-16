package br.coop.voting.domain.voto.repository;

import br.coop.voting.domain.voto.entity.Voto;
import br.coop.voting.domain.voto.entity.VotoEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class VotoRepositoryTest {

    @Autowired
    private VotoRepository votoRepository;

    @Test
    void testFindByPautaIdAndIdAssociado() {
        Voto voto = new Voto();
        voto.setPautaId(1L);
        voto.setSessaoId("sess-1");
        voto.setIdAssociado("A123");
        voto.setVoto(VotoEnum.SIM);
        votoRepository.save(voto);

        Optional<Voto> found = votoRepository.findByPautaIdAndIdAssociado(1L, "A123");

        assertTrue(found.isPresent());
        assertEquals("A123", found.get().getIdAssociado());
        assertEquals(VotoEnum.SIM, found.get().getVoto());
    }

    @Test
    void testFindByPautaIdAndIdAssociado_NotFound() {
        Optional<Voto> found = votoRepository.findByPautaIdAndIdAssociado(999L, "NON_EXISTENT");

        assertFalse(found.isPresent());
    }

    @Test
    void testCountByPautaIdAndVoto_SIM() {
        Voto voto1 = new Voto();
        voto1.setPautaId(1L);
        voto1.setSessaoId("sess-1");
        voto1.setIdAssociado("A123");
        voto1.setVoto(VotoEnum.SIM);

        Voto voto2 = new Voto();
        voto2.setPautaId(1L);
        voto2.setSessaoId("sess-1");
        voto2.setIdAssociado("A456");
        voto2.setVoto(VotoEnum.SIM);

        Voto voto3 = new Voto();
        voto3.setPautaId(1L);
        voto3.setSessaoId("sess-1");
        voto3.setIdAssociado("A789");
        voto3.setVoto(VotoEnum.NAO);

        votoRepository.save(voto1);
        votoRepository.save(voto2);
        votoRepository.save(voto3);

        Long count = votoRepository.countByPautaIdAndVoto(1L, VotoEnum.SIM);

        assertEquals(2L, count);
    }

    @Test
    void testCountByPautaIdAndVoto_NAO() {
        Voto voto1 = new Voto();
        voto1.setPautaId(1L);
        voto1.setSessaoId("sess-1");
        voto1.setIdAssociado("A123");
        voto1.setVoto(VotoEnum.NAO);

        Voto voto2 = new Voto();
        voto2.setPautaId(1L);
        voto2.setSessaoId("sess-1");
        voto2.setIdAssociado("A456");
        voto2.setVoto(VotoEnum.NAO);

        votoRepository.save(voto1);
        votoRepository.save(voto2);

        Long count = votoRepository.countByPautaIdAndVoto(1L, VotoEnum.NAO);

        assertEquals(2L, count);
    }

    @Test
    void testCountByPautaIdAndVoto_Zero() {
        Long count = votoRepository.countByPautaIdAndVoto(999L, VotoEnum.SIM);

        assertEquals(0L, count);
    }

    @Test
    void testUniqueConstraintViolation() {
        Voto voto1 = new Voto();
        voto1.setPautaId(1L);
        voto1.setSessaoId("sess-1");
        voto1.setIdAssociado("A123");
        voto1.setVoto(VotoEnum.SIM);
        votoRepository.save(voto1);

        Voto voto2 = new Voto();
        voto2.setPautaId(1L);
        voto2.setSessaoId("sess-1");
        voto2.setIdAssociado("A123");
        voto2.setVoto(VotoEnum.NAO);

        assertThrows(DataIntegrityViolationException.class, () -> {
            votoRepository.save(voto2);
            votoRepository.flush();
        });
    }

    @Test
    void testSamePautaDifferentAssociados() {
        Voto voto1 = new Voto();
        voto1.setPautaId(1L);
        voto1.setSessaoId("sess-1");
        voto1.setIdAssociado("A123");
        voto1.setVoto(VotoEnum.SIM);

        Voto voto2 = new Voto();
        voto2.setPautaId(1L);
        voto2.setSessaoId("sess-1");
        voto2.setIdAssociado("A456");
        voto2.setVoto(VotoEnum.NAO);

        votoRepository.save(voto1);
        votoRepository.save(voto2);

        assertEquals(2L, votoRepository.count());
    }

    @Test
    void testSameAssociadoDifferentPautas() {
        Voto voto1 = new Voto();
        voto1.setPautaId(1L);
        voto1.setSessaoId("sess-1");
        voto1.setIdAssociado("A123");
        voto1.setVoto(VotoEnum.SIM);

        Voto voto2 = new Voto();
        voto2.setPautaId(2L);
        voto2.setSessaoId("sess-2");
        voto2.setIdAssociado("A123");
        voto2.setVoto(VotoEnum.NAO);

        votoRepository.save(voto1);
        votoRepository.save(voto2);

        assertEquals(2L, votoRepository.count());
    }
}

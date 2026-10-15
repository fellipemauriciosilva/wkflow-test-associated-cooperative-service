package br.coop.assembly.repository;

import br.coop.assembly.entity.Pauta;
import br.coop.assembly.entity.Voto;
import br.coop.assembly.entity.Voto.VotoEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class VotoRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VotoRepository votoRepository;

    @Autowired
    private PautaRepository pautaRepository;

    private Pauta pauta;

    @BeforeEach
    void setUp() {
        pauta = new Pauta();
        pauta.setTitle("Pauta Test");
        pauta.setDescription("Test Description");
        pautaRepository.save(pauta);
        entityManager.flush();
    }

    @Test
    void findByPautaIdAndAssociadoId_WithValidData_ReturnsVoto() {
        Voto voto = new Voto();
        voto.setPautaId(pauta.getId());
        voto.setAssociadoId("A1");
        voto.setVoto(VotoEnum.SIM);
        votoRepository.save(voto);
        entityManager.flush();
        entityManager.clear();

        Optional<Voto> found = votoRepository.findByPautaIdAndAssociadoId(pauta.getId(), "A1");

        assertTrue(found.isPresent());
        assertEquals("A1", found.get().getAssociadoId());
        assertEquals(VotoEnum.SIM, found.get().getVoto());
    }

    @Test
    void findByPautaIdAndAssociadoId_NotFound_ReturnsEmpty() {
        Optional<Voto> found = votoRepository.findByPautaIdAndAssociadoId(pauta.getId(), "NONEXISTENT");

        assertTrue(found.isEmpty());
    }

    @Test
    void findByPautaId_WithMultipleVotes_ReturnsAll() {
        Voto voto1 = new Voto();
        voto1.setPautaId(pauta.getId());
        voto1.setAssociadoId("A1");
        voto1.setVoto(VotoEnum.SIM);
        votoRepository.save(voto1);

        Voto voto2 = new Voto();
        voto2.setPautaId(pauta.getId());
        voto2.setAssociadoId("A2");
        voto2.setVoto(VotoEnum.NAO);
        votoRepository.save(voto2);

        entityManager.flush();
        entityManager.clear();

        List<Voto> votos = votoRepository.findByPautaId(pauta.getId());

        assertEquals(2, votos.size());
    }

    @Test
    void findByPautaId_NoVotes_ReturnsEmptyList() {
        List<Voto> votos = votoRepository.findByPautaId(pauta.getId());

        assertTrue(votos.isEmpty());
    }

    @Test
    void countByPautaIdAndVoto_SIM_ReturnsCorrectCount() {
        Voto voto1 = new Voto();
        voto1.setPautaId(pauta.getId());
        voto1.setAssociadoId("A1");
        voto1.setVoto(VotoEnum.SIM);
        votoRepository.save(voto1);

        Voto voto2 = new Voto();
        voto2.setPautaId(pauta.getId());
        voto2.setAssociadoId("A2");
        voto2.setVoto(VotoEnum.SIM);
        votoRepository.save(voto2);

        Voto voto3 = new Voto();
        voto3.setPautaId(pauta.getId());
        voto3.setAssociadoId("A3");
        voto3.setVoto(VotoEnum.NAO);
        votoRepository.save(voto3);

        entityManager.flush();

        long count = votoRepository.countByPautaIdAndVoto(pauta.getId(), VotoEnum.SIM);

        assertEquals(2, count);
    }

    @Test
    void countByPautaIdAndVoto_NAO_ReturnsCorrectCount() {
        Voto voto1 = new Voto();
        voto1.setPautaId(pauta.getId());
        voto1.setAssociadoId("A1");
        voto1.setVoto(VotoEnum.NAO);
        votoRepository.save(voto1);

        Voto voto2 = new Voto();
        voto2.setPautaId(pauta.getId());
        voto2.setAssociadoId("A2");
        voto2.setVoto(VotoEnum.SIM);
        votoRepository.save(voto2);

        entityManager.flush();

        long count = votoRepository.countByPautaIdAndVoto(pauta.getId(), VotoEnum.NAO);

        assertEquals(1, count);
    }

    @Test
    void countByPautaIdAndVoto_NoVotes_ReturnsZero() {
        long count = votoRepository.countByPautaIdAndVoto(pauta.getId(), VotoEnum.SIM);

        assertEquals(0, count);
    }

    @Test
    void uniqueConstraint_DuplicateVote_Fails() {
        Voto voto1 = new Voto();
        voto1.setPautaId(pauta.getId());
        voto1.setAssociadoId("A1");
        voto1.setVoto(VotoEnum.SIM);
        votoRepository.save(voto1);
        entityManager.flush();

        Voto voto2 = new Voto();
        voto2.setPautaId(pauta.getId());
        voto2.setAssociadoId("A1");
        voto2.setVoto(VotoEnum.NAO);

        assertThrows(Exception.class, () -> {
            votoRepository.save(voto2);
            entityManager.flush();
        });
    }
}

package br.coop.assembly.repository;

import br.coop.assembly.entity.Pauta;
import br.coop.assembly.entity.Sessao;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("SessaoRepository Tests")
class SessaoRepositoryTest {
    @Autowired
    private SessaoRepository sessaoRepository;

    @Autowired
    private PautaRepository pautaRepository;

    @Autowired
    private EntityManager entityManager;

    private Pauta testPauta;
    private Sessao testSessao;

    @BeforeEach
    void setUp() {
        testPauta = new Pauta();
        testPauta.setTitle("Test Pauta");
        testPauta.setDescription("Test Description");
        testPauta.setIsDeleted(false);
        testPauta = pautaRepository.save(testPauta);

        testSessao = new Sessao();
        testSessao.setPauta(testPauta);
        testSessao.setPautaId(testPauta.getId());
        testSessao.setDataInicio(LocalDateTime.now());
        testSessao.setDataFim(LocalDateTime.now().plusMinutes(10));
        testSessao.setStatus(Sessao.Status.ATIVA);
    }

    @Test
    @DisplayName("Should persist and retrieve session by id")
    void save_PersistsSessionToH2() {
        Sessao saved = sessaoRepository.save(testSessao);
        sessaoRepository.flush();
        entityManager.clear();

        Optional<Sessao> retrieved = sessaoRepository.findById(saved.getId());

        assertTrue(retrieved.isPresent());
        assertEquals(saved.getId(), retrieved.get().getId());
        assertEquals(testPauta.getId(), retrieved.get().getPautaId());
        assertEquals(Sessao.Status.ATIVA, retrieved.get().getStatus());
    }

    @Test
    @DisplayName("Should find session by pautaId")
    void findByPautaId_ReturnsActiveSession() {
        sessaoRepository.save(testSessao);
        sessaoRepository.flush();

        Optional<Sessao> retrieved = sessaoRepository.findByPautaId(testPauta.getId());

        assertTrue(retrieved.isPresent());
        assertEquals(testPauta.getId(), retrieved.get().getPautaId());
    }

    @Test
    @DisplayName("Should return empty if no session for pautaId")
    void findByPautaId_ReturnsEmptyIfNoSession() {
        Optional<Sessao> retrieved = sessaoRepository.findByPautaId(999L);

        assertTrue(retrieved.isEmpty());
    }

    @Test
    @DisplayName("Should find active session for pauta with current time check")
    void findActiveSessaoForPauta_ReturnsActiveSessao() {
        LocalDateTime now = LocalDateTime.now();
        testSessao.setDataInicio(now);
        testSessao.setDataFim(now.plusMinutes(10));
        sessaoRepository.save(testSessao);
        sessaoRepository.flush();

        Optional<Sessao> retrieved = sessaoRepository.findActiveSessaoForPauta(testPauta.getId(), now);

        assertTrue(retrieved.isPresent());
        assertEquals(testPauta.getId(), retrieved.get().getPautaId());
        assertEquals(Sessao.Status.ATIVA, retrieved.get().getStatus());
    }

    @Test
    @DisplayName("Should not return expired session")
    void findActiveSessaoForPauta_IgnoresExpiredSessao() {
        LocalDateTime now = LocalDateTime.now();
        testSessao.setDataInicio(now.minusMinutes(15));
        testSessao.setDataFim(now.minusMinutes(5));
        testSessao.setStatus(Sessao.Status.EXPIRADA);
        sessaoRepository.save(testSessao);
        sessaoRepository.flush();

        Optional<Sessao> retrieved = sessaoRepository.findActiveSessaoForPauta(testPauta.getId(), now);

        assertTrue(retrieved.isEmpty());
    }

    @Test
    @DisplayName("Should handle multiple sessions for different pautas")
    void save_MultipleSessions_ForDifferentPautas() {
        Pauta pauta2 = new Pauta();
        pauta2.setTitle("Another Pauta");
        pauta2.setDescription("Description 2");
        pauta2.setIsDeleted(false);
        pauta2 = pautaRepository.save(pauta2);

        Sessao sessao2 = new Sessao();
        sessao2.setPauta(pauta2);
        sessao2.setPautaId(pauta2.getId());
        sessao2.setDataInicio(LocalDateTime.now());
        sessao2.setDataFim(LocalDateTime.now().plusMinutes(5));
        sessao2.setStatus(Sessao.Status.ATIVA);

        Sessao saved1 = sessaoRepository.save(testSessao);
        Sessao saved2 = sessaoRepository.save(sessao2);
        sessaoRepository.flush();

        Optional<Sessao> retrieved1 = sessaoRepository.findByPautaId(testPauta.getId());
        Optional<Sessao> retrieved2 = sessaoRepository.findByPautaId(pauta2.getId());

        assertTrue(retrieved1.isPresent());
        assertTrue(retrieved2.isPresent());
        assertEquals(testPauta.getId(), retrieved1.get().getPautaId());
        assertEquals(pauta2.getId(), retrieved2.get().getPautaId());
    }
}

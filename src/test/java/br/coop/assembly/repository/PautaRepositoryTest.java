package br.coop.assembly.repository;

import br.coop.assembly.entity.Pauta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("PautaRepository Tests")
class PautaRepositoryTest {
    @Autowired
    private PautaRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private Pauta testPauta;

    @BeforeEach
    void setUp() {
        testPauta = new Pauta();
        testPauta.setTitle("Test Pauta");
        testPauta.setDescription("Test Description");
        testPauta.setIsDeleted(false);
    }

    @Test
    @DisplayName("Should persist pauta to H2 and generate id")
    void save_ValidPauta_PersistsToH2() {
        Pauta saved = repository.save(testPauta);
        entityManager.flush();

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        Pauta retrieved = entityManager.find(Pauta.class, saved.getId());
        assertNotNull(retrieved);
        assertEquals("Test Pauta", retrieved.getTitle());
        assertEquals("Test Description", retrieved.getDescription());
    }

    @Test
    @DisplayName("Should find pauta by existing id")
    void findById_ExistingId_ReturnsPauta() {
        Pauta saved = repository.save(testPauta);
        entityManager.flush();

        Optional<Pauta> result = repository.findById(saved.getId());

        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.get().getId());
        assertEquals("Test Pauta", result.get().getTitle());
    }

    @Test
    @DisplayName("Should return empty optional for non-existent id")
    void findById_NonExistentId_ReturnsEmptyOptional() {
        Optional<Pauta> result = repository.findById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return all persisted pautas")
    void findAll_MultiplePersistedPautas_ReturnsAllRecords() {
        Pauta pauta1 = new Pauta();
        pauta1.setTitle("First Pauta");
        pauta1.setDescription("First Description");
        pauta1.setIsDeleted(false);

        Pauta pauta2 = new Pauta();
        pauta2.setTitle("Second Pauta");
        pauta2.setDescription("Second Description");
        pauta2.setIsDeleted(false);

        Pauta pauta3 = new Pauta();
        pauta3.setTitle("Third Pauta");
        pauta3.setDescription("Third Description");
        pauta3.setIsDeleted(false);

        repository.save(pauta1);
        repository.save(pauta2);
        repository.save(pauta3);
        entityManager.flush();

        List<Pauta> result = repository.findAll();

        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(p -> p.getTitle().equals("First Pauta")));
        assertTrue(result.stream().anyMatch(p -> p.getTitle().equals("Second Pauta")));
        assertTrue(result.stream().anyMatch(p -> p.getTitle().equals("Third Pauta")));
    }

    @Test
    @DisplayName("Should delete pauta from H2")
    void delete_ExistingPauta_RemovesFromH2() {
        Pauta saved = repository.save(testPauta);
        entityManager.flush();

        repository.delete(saved);
        entityManager.flush();

        Optional<Pauta> result = repository.findById(saved.getId());
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should delete pauta by id")
    void deleteById_ExistingId_RemovesFromH2() {
        Pauta saved = repository.save(testPauta);
        entityManager.flush();

        repository.deleteById(saved.getId());
        entityManager.flush();

        Optional<Pauta> result = repository.findById(saved.getId());
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should update existing pauta and persist changes")
    void update_ModifyExistingPauta_PersistsChanges() {
        Pauta saved = repository.save(testPauta);
        entityManager.flush();

        saved.setTitle("Updated Title");
        Pauta updated = repository.save(saved);
        entityManager.flush();

        Pauta retrieved = entityManager.find(Pauta.class, updated.getId());
        assertEquals("Updated Title", retrieved.getTitle());
        assertNotNull(retrieved.getUpdatedAt());
    }

    @Test
    @DisplayName("Should maintain created and updated timestamps correctly")
    void timestamps_CreatedAtImmutableUpdatedAtMutable() {
        Pauta saved = repository.save(testPauta);
        entityManager.flush();

        Pauta retrieved = repository.findById(saved.getId()).orElseThrow();
        assertNotNull(retrieved.getCreatedAt());
        assertNotNull(retrieved.getUpdatedAt());
        LocalDateTime createdAt = retrieved.getCreatedAt();
        LocalDateTime initialUpdatedAt = retrieved.getUpdatedAt();

        // Update and verify timestamps
        retrieved.setTitle("Modified");
        repository.save(retrieved);
        entityManager.flush();
        entityManager.clear();

        Pauta updated = repository.findById(saved.getId()).orElseThrow();
        assertEquals(createdAt, updated.getCreatedAt());
        // updatedAt should be >= initial
        assertTrue(updated.getUpdatedAt().isAfter(initialUpdatedAt) ||
                updated.getUpdatedAt().isEqual(initialUpdatedAt));
    }
}

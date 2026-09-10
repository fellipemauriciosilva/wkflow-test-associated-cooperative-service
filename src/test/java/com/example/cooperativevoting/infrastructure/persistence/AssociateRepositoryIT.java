package com.example.cooperativevoting.infrastructure.persistence;

import com.example.cooperativevoting.domain.entities.Associate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AssociateRepositoryIT {

    @Autowired
    private AssociateRepository repository;

    @Test
    void testSaveAndFindAssociate() {
        UUID id = UUID.randomUUID();
        Associate associate = new Associate(id, "John Doe", "doc-001", "john@example.com");

        Associate saved = repository.save(associate);

        assertEquals(id, saved.getId());
        assertEquals("John Doe", saved.getName());

        Optional<Associate> found = repository.findById(id);
        assertTrue(found.isPresent());
        assertEquals("John Doe", found.get().getName());
    }

    @Test
    void testFindByDocument() {
        Associate associate = new Associate(UUID.randomUUID(), "John", "doc-unique-001", "john@example.com");
        repository.save(associate);

        Optional<Associate> found = repository.findByDocument("doc-unique-001");

        assertTrue(found.isPresent());
        assertEquals("John", found.get().getName());
    }

    @Test
    void testFindByDocumentNotFound() {
        Optional<Associate> found = repository.findByDocument("non-existent");
        assertFalse(found.isPresent());
    }

    @Test
    void testDuplicateDocumentThrowsConstraintViolation() {
        Associate associate1 = new Associate(UUID.randomUUID(), "John", "duplicate-doc", "john@example.com");
        Associate associate2 = new Associate(UUID.randomUUID(), "Jane", "duplicate-doc", "jane@example.com");

        repository.save(associate1);

        assertThrows(DataIntegrityViolationException.class, () -> {
            repository.saveAndFlush(associate2);
        });
    }

    @Test
    void testUpdateAssociate() {
        Associate associate = new Associate(UUID.randomUUID(), "John", "doc-002", "john@example.com");
        repository.save(associate);

        associate.setName("Jane");
        repository.save(associate);

        Optional<Associate> found = repository.findById(associate.getId());
        assertTrue(found.isPresent());
        assertEquals("Jane", found.get().getName());
    }

    @Test
    void testDeleteAssociate() {
        Associate associate = new Associate(UUID.randomUUID(), "John", "doc-003", "john@example.com");
        Associate saved = repository.save(associate);

        repository.delete(saved);

        Optional<Associate> found = repository.findById(saved.getId());
        assertFalse(found.isPresent());
    }
}

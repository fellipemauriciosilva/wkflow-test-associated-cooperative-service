package com.cooperative.infrastructure.adapters.repository;

import com.cooperative.domain.entity.Associate;
import com.cooperative.infrastructure.adapters.repository.JpaAssociateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class AssociateRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("test_db")
        .withUsername("testuser")
        .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JpaAssociateRepository repository;

    @Test
    void testSaveAndFindById() {
        Associate associate = Associate.create("John Doe", "12345678901", "john@example.com");

        Associate saved = repository.save(associate);

        assertNotNull(saved.id());
        assertEquals("John Doe", saved.name());

        Optional<Associate> retrieved = repository.findById(saved.id());
        assertTrue(retrieved.isPresent());
        assertEquals("John Doe", retrieved.get().name());
    }

    @Test
    void testFindByDocument() {
        Associate associate = Associate.create("John Doe", "12345678901", "john@example.com");
        repository.save(associate);

        Optional<Associate> found = repository.findByDocument("12345678901");

        assertTrue(found.isPresent());
        assertEquals("John Doe", found.get().name());
    }

    @Test
    void testFindByDocumentNotFound() {
        Optional<Associate> found = repository.findByDocument("nonexistent");
        assertFalse(found.isPresent());
    }

    @Test
    void testFindAll() {
        Associate a1 = Associate.create("John Doe", "111", "john@example.com");
        Associate a2 = Associate.create("Jane Doe", "222", "jane@example.com");
        repository.save(a1);
        repository.save(a2);

        List<Associate> all = repository.findAll();

        assertTrue(all.size() >= 2);
    }

    @Test
    void testDuplicateDocumentThrowsException() {
        Associate a1 = Associate.create("John Doe", "12345678901", "john@example.com");
        Associate a2 = Associate.create("Jane Doe", "12345678901", "jane@example.com");

        repository.save(a1);

        assertThrows(DataIntegrityViolationException.class, () -> {
            repository.save(a2);
        });
    }

    @Test
    void testFindByIdNotFound() {
        Optional<Associate> found = repository.findById(99999L);
        assertFalse(found.isPresent());
    }
}

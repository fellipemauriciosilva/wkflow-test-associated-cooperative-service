package com.cooperative.infrastructure.adapters.repository;

import com.cooperative.domain.entity.Assembly;
import com.cooperative.infrastructure.adapters.repository.JpaAssemblyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class AssemblyRepositoryIT {

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
    private JpaAssemblyRepository repository;

    @Test
    void testSaveAndFindById() {
        Assembly assembly = Assembly.create(
            "Board Meeting", "Quarterly review", Instant.now());

        Assembly saved = repository.save(assembly);

        assertNotNull(saved.id());
        assertEquals("Board Meeting", saved.title());

        Optional<Assembly> retrieved = repository.findById(saved.id());
        assertTrue(retrieved.isPresent());
    }

    @Test
    void testFindAll() {
        Assembly a1 = Assembly.create("Meeting 1", "Desc 1", Instant.now());
        Assembly a2 = Assembly.create("Meeting 2", null, null);
        repository.save(a1);
        repository.save(a2);

        List<Assembly> all = repository.findAll();

        assertTrue(all.size() >= 2);
    }

    @Test
    void testFindByIdNotFound() {
        Optional<Assembly> found = repository.findById(99999L);
        assertFalse(found.isPresent());
    }
}

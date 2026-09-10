package com.example.cooperativevoting.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class FlywayMigrationIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testAllMigrationsApplied() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'",
                Integer.class);

        assertNotNull(tableCount);
        assertTrue(tableCount >= 5, "Expected at least 5 tables, found: " + tableCount);
    }

    @Test
    void testAssociatesTableExists() {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name='associates')",
                Boolean.class);

        assertTrue(exists);
    }

    @Test
    void testAssembliesTableExists() {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name='assemblies')",
                Boolean.class);

        assertTrue(exists);
    }

    @Test
    void testAgendasTableExists() {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name='agendas')",
                Boolean.class);

        assertTrue(exists);
    }

    @Test
    void testVotesTableExists() {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name='votes')",
                Boolean.class);

        assertTrue(exists);
    }

    @Test
    void testShedlockTableExists() {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name='shedlock')",
                Boolean.class);

        assertTrue(exists);
    }

    @Test
    void testAssociatesTableHasExpectedColumns() {
        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name='associates'",
                Integer.class);

        assertTrue(columnCount >= 6, "Expected at least 6 columns in associates, found: " + columnCount);
    }

    @Test
    void testDocumentUniqueConstraintExists() {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM information_schema.constraint_column_usage " +
                "WHERE table_name='associates' AND column_name='document')",
                Boolean.class);

        assertTrue(exists);
    }

    @Test
    void testAgendasForeignKeyConstraintExists() {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM information_schema.referential_constraints " +
                "WHERE constraint_name='fk_agendas_assembly_id')",
                Boolean.class);

        assertTrue(exists);
    }

    @Test
    void testVotesForeignKeyConstraintsExist() {
        Boolean agendaFk = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM information_schema.referential_constraints " +
                "WHERE constraint_name='fk_votes_agenda_id')",
                Boolean.class);

        Boolean associateFk = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM information_schema.referential_constraints " +
                "WHERE constraint_name='fk_votes_associate_id')",
                Boolean.class);

        assertTrue(agendaFk);
        assertTrue(associateFk);
    }

    @Test
    void testIndexesCreated() {
        Integer indexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE tablename IN ('associates', 'assemblies', 'agendas', 'votes')",
                Integer.class);

        assertTrue(indexCount >= 8, "Expected at least 8 indexes, found: " + indexCount);
    }
}

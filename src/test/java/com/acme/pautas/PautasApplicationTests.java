package com.acme.pautas;

import com.acme.pautas.repository.PautaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class PautasApplicationTests {

    @Autowired
    private PautaRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void contextoSobeComH2EmMemoriaEComTabelaDePautas() {
        assertNotNull(repository);
        Integer existe = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = 'PAUTA'",
                Integer.class);
        assertNotNull(existe);
        org.junit.jupiter.api.Assertions.assertEquals(1, existe);
    }
}

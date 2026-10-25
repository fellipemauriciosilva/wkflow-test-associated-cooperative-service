package com.acme.pautas.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VotosProducaoGuardTest {

    @Test
    void recusaVotosHabilitadosNoPerfilProd() {
        MockEnvironment ambiente = new MockEnvironment().withProperty("spring.profiles.active", "prod");

        assertThrows(IllegalStateException.class,
                () -> new VotosProducaoGuard(ambiente, true).afterPropertiesSet());
    }

    @Test
    void aceitaVotosDesabilitadosNoPerfilProd() {
        MockEnvironment ambiente = new MockEnvironment().withProperty("spring.profiles.active", "prod");

        assertDoesNotThrow(() -> new VotosProducaoGuard(ambiente, false).afterPropertiesSet());
    }

    @Test
    void aceitaVotosHabilitadosForaDeProd() {
        MockEnvironment ambiente = new MockEnvironment();

        assertDoesNotThrow(() -> new VotosProducaoGuard(ambiente, true).afterPropertiesSet());
    }
}

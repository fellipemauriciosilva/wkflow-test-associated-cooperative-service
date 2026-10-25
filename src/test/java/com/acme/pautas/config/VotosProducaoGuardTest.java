package com.acme.pautas.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VotosProducaoGuardTest {

    @Test
    void recusaVotosHabilitadosNoPerfilProd() {
        assertRecusa(ambienteCom("prod"), true);
    }

    @Test
    void recusaVotosHabilitadosNoPerfilProduction() {
        assertRecusa(ambienteCom("production"), true);
    }

    @Test
    void recusaVotosHabilitadosQuandoAlgumPerfilNaoEhPermitido() {
        assertRecusa(ambienteCom("test", "prod"), true);
    }

    @Test
    void recusaVotosHabilitadosSemPerfilAtivo() {
        assertRecusa(new MockEnvironment(), true);
    }

    @Test
    void aceitaVotosDesabilitadosNoPerfilProd() {
        assertDoesNotThrow(() -> new VotosProducaoGuard(ambienteCom("prod"), false).afterPropertiesSet());
    }

    @Test
    void aceitaVotosDesabilitadosSemPerfilAtivo() {
        assertDoesNotThrow(() -> new VotosProducaoGuard(new MockEnvironment(), false).afterPropertiesSet());
    }

    @Test
    void aceitaVotosHabilitadosNosPerfisPermitidos() {
        for (String perfil : new String[]{"dev", "test", "local"}) {
            assertDoesNotThrow(() -> new VotosProducaoGuard(ambienteCom(perfil), true).afterPropertiesSet(),
                    perfil);
        }
    }

    private static void assertRecusa(MockEnvironment ambiente, boolean habilitado) {
        assertThrows(IllegalStateException.class,
                () -> new VotosProducaoGuard(ambiente, habilitado).afterPropertiesSet());
    }

    private static MockEnvironment ambienteCom(String... perfis) {
        return new MockEnvironment().withProperty("spring.profiles.active", String.join(",", perfis));
    }
}

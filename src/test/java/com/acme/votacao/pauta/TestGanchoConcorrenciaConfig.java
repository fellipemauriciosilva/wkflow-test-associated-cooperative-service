package com.acme.votacao.pauta;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestGanchoConcorrenciaConfig {

    @Bean
    @Primary
    public GanchoConcorrenciaSessaoControlavel ganchoConcorrenciaSessaoControlavel() {
        return new GanchoConcorrenciaSessaoControlavel();
    }
}

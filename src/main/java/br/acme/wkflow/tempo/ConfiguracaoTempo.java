package br.acme.wkflow.tempo;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoTempo {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}

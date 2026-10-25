package br.acme.pauta;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RelogioConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}

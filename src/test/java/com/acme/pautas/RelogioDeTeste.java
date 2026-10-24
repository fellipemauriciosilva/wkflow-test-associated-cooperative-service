package com.acme.pautas;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@TestConfiguration
public class RelogioDeTeste {

    public static final Instant INICIO = Instant.parse("2026-10-24T12:00:00Z");

    @Bean
    @Primary
    public RelogioControlavel relogioControlavel() {
        return new RelogioControlavel(INICIO);
    }

    public static class RelogioControlavel extends Clock {

        private Instant agora;

        public RelogioControlavel(Instant agora) {
            this.agora = agora;
        }

        public void ajustarPara(Instant novoAgora) {
            this.agora = novoAgora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}

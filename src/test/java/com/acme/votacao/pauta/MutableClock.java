package com.acme.votacao.pauta;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** Clock mutável usado só em testes para controlar "agora" de forma determinística. */
public class MutableClock extends Clock {

    private volatile Instant instante;
    private final ZoneId zona;

    public MutableClock(Instant instante, ZoneId zona) {
        this.instante = instante;
        this.zona = zona;
    }

    public void definir(Instant novoInstante) {
        this.instante = novoInstante;
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(instante, zone);
    }

    @Override
    public Instant instant() {
        return instante;
    }
}

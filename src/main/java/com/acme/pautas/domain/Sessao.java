package com.acme.pautas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity
public class Sessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pauta_id", nullable = false)
    private Long pautaId;

    @Column(nullable = false)
    private Instant abertura;

    @Column(nullable = false)
    private Instant fim;

    protected Sessao() {
    }

    public Sessao(Long pautaId, Instant abertura, Instant fim) {
        this.pautaId = pautaId;
        this.abertura = abertura;
        this.fim = fim;
    }

    public Long getId() {
        return id;
    }

    public Long getPautaId() {
        return pautaId;
    }

    public Instant getAbertura() {
        return abertura;
    }

    public Instant getFim() {
        return fim;
    }
}

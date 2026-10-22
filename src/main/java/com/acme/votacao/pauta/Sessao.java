package com.acme.votacao.pauta;

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
    private Instant inicio;

    @Column(nullable = false)
    private Instant fim;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoMinutos;

    protected Sessao() {
    }

    public Sessao(Long pautaId, Instant inicio, Instant fim, Integer duracaoMinutos) {
        this.pautaId = pautaId;
        this.inicio = inicio;
        this.fim = fim;
        this.duracaoMinutos = duracaoMinutos;
    }

    public Long getId() {
        return id;
    }

    public Long getPautaId() {
        return pautaId;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFim() {
        return fim;
    }

    public Integer getDuracaoMinutos() {
        return duracaoMinutos;
    }
}

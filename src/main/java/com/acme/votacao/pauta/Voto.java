package com.acme.votacao.pauta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"pauta_id", "associado_id"}))
public class Voto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pauta_id", nullable = false)
    private Long pautaId;

    @Column(name = "associado_id", nullable = false)
    private String associadoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VotoValor voto;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Voto() {
    }

    public Voto(Long pautaId, String associadoId, VotoValor voto, Instant criadoEm) {
        this.pautaId = pautaId;
        this.associadoId = associadoId;
        this.voto = voto;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public Long getPautaId() {
        return pautaId;
    }

    public String getAssociadoId() {
        return associadoId;
    }

    public VotoValor getVoto() {
        return voto;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}

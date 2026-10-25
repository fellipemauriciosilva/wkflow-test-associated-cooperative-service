package br.acme.pauta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;

@Entity
public class Sessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pauta_id", nullable = false)
    private Pauta pauta;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fim;

    protected Sessao() {
    }

    public Sessao(Pauta pauta, Instant inicio, Instant fim) {
        this.pauta = pauta;
        this.inicio = inicio;
        this.fim = fim;
    }

    public Long getId() {
        return id;
    }

    public Pauta getPauta() {
        return pauta;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFim() {
        return fim;
    }
}

package com.votacao.domain.sessao;

import com.votacao.domain.pauta.Pauta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessao", uniqueConstraints = @UniqueConstraint(name = Sessao.UK_PAUTA_ID, columnNames = "pauta_id"))
public class Sessao {

    public static final String UK_PAUTA_ID = "uk_sessao_pauta_id";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "pauta_id", nullable = false)
    private Pauta pauta;

    @Column(nullable = false)
    private LocalDateTime abertura;

    @Column(nullable = false)
    private LocalDateTime fechamento;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoMinutos;

    protected Sessao() {
        // JPA
    }

    public Sessao(Pauta pauta, LocalDateTime abertura, Integer duracaoMinutos) {
        this.pauta = pauta;
        this.abertura = abertura;
        this.duracaoMinutos = duracaoMinutos;
        this.fechamento = abertura.plusMinutes(duracaoMinutos);
    }

    public Long getId() {
        return id;
    }

    public Pauta getPauta() {
        return pauta;
    }

    public LocalDateTime getAbertura() {
        return abertura;
    }

    public LocalDateTime getFechamento() {
        return fechamento;
    }

    public Integer getDuracaoMinutos() {
        return duracaoMinutos;
    }
}

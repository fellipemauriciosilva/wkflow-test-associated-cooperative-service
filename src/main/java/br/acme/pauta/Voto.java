package br.acme.pauta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "voto", uniqueConstraints =
        @UniqueConstraint(name = "uk_voto_pauta_associado", columnNames = {"pauta_id", "associado_id"}))
public class Voto {

    static final int TAMANHO_ASSOCIADO = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pauta_id", nullable = false)
    private Pauta pauta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessao_id", nullable = false)
    private Sessao sessao;

    @Column(name = "associado_id", nullable = false, length = TAMANHO_ASSOCIADO)
    private String associadoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpcaoVoto opcao;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Voto() {
    }

    public Voto(Pauta pauta, Sessao sessao, String associadoId, OpcaoVoto opcao, Instant criadoEm) {
        this.pauta = pauta;
        this.sessao = sessao;
        this.associadoId = associadoId;
        this.opcao = opcao;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public Pauta getPauta() {
        return pauta;
    }

    public Sessao getSessao() {
        return sessao;
    }

    public String getAssociadoId() {
        return associadoId;
    }

    public OpcaoVoto getOpcao() {
        return opcao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}

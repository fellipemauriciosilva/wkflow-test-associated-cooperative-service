package com.votacao.domain.voto;

import com.votacao.domain.pauta.Pauta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "voto", uniqueConstraints = @UniqueConstraint(
        name = Voto.UK_PAUTA_ASSOCIADO, columnNames = {"pauta_id", "id_associado"}))
public class Voto {

    public static final String UK_PAUTA_ASSOCIADO = "uk_voto_pauta_associado";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pauta_id", nullable = false)
    private Pauta pauta;

    @Column(name = "id_associado", nullable = false)
    private Long idAssociado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private TipoVoto tipo;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime criacao;

    protected Voto() {
        // JPA
    }

    public Voto(Pauta pauta, Long idAssociado, TipoVoto tipo) {
        this.pauta = pauta;
        this.idAssociado = idAssociado;
        this.tipo = tipo;
    }

    public Long getId() {
        return id;
    }

    public Pauta getPauta() {
        return pauta;
    }

    public Long getIdAssociado() {
        return idAssociado;
    }

    public TipoVoto getTipo() {
        return tipo;
    }

    public LocalDateTime getCriacao() {
        return criacao;
    }
}

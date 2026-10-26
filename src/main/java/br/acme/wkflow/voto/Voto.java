package br.acme.wkflow.voto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "voto", uniqueConstraints = @UniqueConstraint(name = Voto.UK_PAUTA_ASSOCIADO, columnNames = {"pauta_id", "associado_id"}))
public class Voto {

    public static final String UK_PAUTA_ASSOCIADO = "uk_voto_pauta_associado";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pauta_id", nullable = false)
    private Long pautaId;

    @Column(name = "associado_id", nullable = false, length = 255)
    private String associadoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private ValorVoto valor;

    protected Voto() {
    }

    public Voto(Long pautaId, String associadoId, ValorVoto valor) {
        this.pautaId = pautaId;
        this.associadoId = associadoId;
        this.valor = valor;
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

    public ValorVoto getValor() {
        return valor;
    }
}

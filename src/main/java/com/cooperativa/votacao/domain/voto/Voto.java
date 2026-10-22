package com.cooperativa.votacao.domain.voto;

import com.cooperativa.votacao.domain.pauta.Pauta;
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
import java.util.Objects;

@Entity
@Table(
        name = "voto",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_voto_associado_pauta", columnNames = {"associado", "pauta_id"}))
public class Voto {

    public static final int ASSOCIADO_TAMANHO_MAXIMO = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pauta_id", nullable = false)
    private Pauta pauta;

    @Column(nullable = false, length = ASSOCIADO_TAMANHO_MAXIMO)
    private String associado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_voto", nullable = false)
    private TipoVoto tipoVoto;

    protected Voto() {
        // exigido pelo JPA
    }

    public Voto(Pauta pauta, String associado, TipoVoto tipoVoto) {
        validarAssociado(associado);
        validarTipoVoto(tipoVoto);
        this.pauta = Objects.requireNonNull(pauta, "pauta e obrigatoria");
        this.associado = associado.strip();
        this.tipoVoto = tipoVoto;
    }

    private static void validarAssociado(String associado) {
        if (associado == null || associado.isBlank()) {
            throw new VotoInvalidoException("associado e obrigatorio");
        }
        var associadoStripado = associado.strip();
        if (associadoStripado.codePointCount(0, associadoStripado.length()) > ASSOCIADO_TAMANHO_MAXIMO) {
            throw new VotoInvalidoException(
                    "associado deve ter no maximo " + ASSOCIADO_TAMANHO_MAXIMO + " caracteres");
        }
    }

    private static void validarTipoVoto(TipoVoto tipoVoto) {
        if (tipoVoto == null) {
            throw new VotoInvalidoException("voto e obrigatorio e deve ser SIM ou NAO");
        }
    }

    public Long getId() {
        return id;
    }

    public Pauta getPauta() {
        return pauta;
    }

    public String getAssociado() {
        return associado;
    }

    public TipoVoto getTipoVoto() {
        return tipoVoto;
    }
}

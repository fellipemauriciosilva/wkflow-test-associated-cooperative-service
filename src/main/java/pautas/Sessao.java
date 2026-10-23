package pautas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "sessao", uniqueConstraints = @UniqueConstraint(name = Sessao.UK_PAUTA, columnNames = "pauta_id"))
public class Sessao {

    static final String UK_PAUTA = "uk_sessao_pauta";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sessao_seq")
    @SequenceGenerator(name = "sessao_seq", sequenceName = "sessao_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pauta_id", nullable = false)
    private Pauta pauta;

    @Column(name = "abertura", nullable = false)
    private Instant abertura;

    @Column(name = "fim", nullable = false)
    private Instant fim;

    protected Sessao() {
    }

    public Sessao(Pauta pauta, Instant abertura, Instant fim) {
        this.pauta = pauta;
        this.abertura = abertura;
        this.fim = fim;
    }

    public Long getId() {
        return id;
    }

    public Pauta getPauta() {
        return pauta;
    }

    public Instant getAbertura() {
        return abertura;
    }

    public Instant getFim() {
        return fim;
    }
}

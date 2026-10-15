package br.coop.assembly.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "voto", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"pauta_id", "associado_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Voto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pauta_id", nullable = false)
    private Pauta pauta;

    @Column(nullable = false)
    private String associadoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VotoEnum voto;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    public enum VotoEnum {
        SIM, NAO
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Voto v = (Voto) o;
        return id != null && id.equals(v.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Voto{" +
                "id=" + id +
                ", associadoId='" + associadoId + '\'' +
                ", voto=" + voto +
                ", timestamp=" + timestamp +
                '}';
    }
}

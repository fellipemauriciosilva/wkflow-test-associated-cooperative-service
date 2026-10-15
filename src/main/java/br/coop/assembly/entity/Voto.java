package br.coop.assembly.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "votos", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"pauta_id", "associado_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Voto {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "pauta_id", nullable = false)
    private Long pautaId;

    @Column(name = "associado_id", nullable = false, length = 255)
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
        Voto vote = (Voto) o;
        return id != null && id.equals(vote.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Voto{" +
                "id=" + id +
                ", pautaId=" + pautaId +
                ", associadoId='" + associadoId + '\'' +
                ", voto=" + voto +
                ", timestamp=" + timestamp +
                '}';
    }
}

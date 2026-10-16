package br.coop.voting.domain.voto.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "votos", uniqueConstraints = @UniqueConstraint(columnNames = {"pauta_id", "id_associado"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {})
public class Voto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pauta_id", nullable = false)
    private Long pautaId;

    @Column(name = "sessao_id", nullable = false)
    private String sessaoId;

    @Column(name = "id_associado", nullable = false)
    private String idAssociado;

    @Column(name = "voto", nullable = false)
    @Enumerated(EnumType.STRING)
    private VotoEnum voto;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

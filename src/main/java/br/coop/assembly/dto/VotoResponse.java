package br.coop.assembly.dto;

import br.coop.assembly.entity.Voto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VotoResponse {
    private Long id;
    private Long pautaId;
    private String associadoId;
    private Voto.VotoEnum voto;
    private LocalDateTime timestamp;
}

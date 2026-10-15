package br.coop.assembly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoDTO {
    private Long pautaId;
    private Long totalVotos;
    private Long votosSim;
    private Long votosNao;
    private BigDecimal percentualSim;
    private BigDecimal percentualNao;
    private String status;
}

package br.coop.assembly.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoDTO {
    private int totalVotos;
    private int votosSim;
    private int votosNao;
    private BigDecimal percentualSim;
    private BigDecimal percentualNao;
    private String status;

    public static ResultadoDTO calcular(int votosSim, int votosNao, String status) {
        int total = votosSim + votosNao;
        ResultadoDTO dto = new ResultadoDTO();
        dto.setTotalVotos(total);
        dto.setVotosSim(votosSim);
        dto.setVotosNao(votosNao);

        if (total == 0) {
            dto.setPercentualSim(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            dto.setPercentualNao(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        } else {
            BigDecimal totalBD = BigDecimal.valueOf(total);
            BigDecimal simBD = BigDecimal.valueOf(votosSim);
            BigDecimal naoBD = BigDecimal.valueOf(votosNao);

            BigDecimal percSim = simBD.divide(totalBD, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal percNao = naoBD.divide(totalBD, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

            dto.setPercentualSim(percSim);
            dto.setPercentualNao(percNao);
        }

        dto.setStatus(status);
        return dto;
    }
}

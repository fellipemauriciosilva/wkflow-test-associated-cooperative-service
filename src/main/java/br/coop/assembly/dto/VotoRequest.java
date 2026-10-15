package br.coop.assembly.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VotoRequest {
    @NotBlank(message = "Associado ID é obrigatório")
    private String associadoId;

    @NotBlank(message = "Voto é obrigatório")
    private String voto;
}

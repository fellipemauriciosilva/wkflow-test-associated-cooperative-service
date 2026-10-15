package br.coop.assembly.dto;

import br.coop.assembly.entity.Voto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarVotoRequest {
    @NotBlank(message = "AssociadoId obrigatório")
    private String associadoId;

    @NotNull(message = "Voto obrigatório")
    private Voto.VotoEnum voto;
}

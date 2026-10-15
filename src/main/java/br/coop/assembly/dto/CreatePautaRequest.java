package br.coop.assembly.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePautaRequest {
    @NotBlank(message = "Título é obrigatório")
    @Size(min = 3, max = 255, message = "Título deve ter entre 3 e 255 caracteres")
    private String title;

    @Size(max = 1000, message = "Descrição não pode exceder 1000 caracteres")
    private String description;
}

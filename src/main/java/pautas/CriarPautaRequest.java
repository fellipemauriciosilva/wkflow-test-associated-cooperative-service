package pautas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarPautaRequest(
        @NotBlank(message = "não pode ser vazio")
        @Size(max = 200, message = "não pode ter mais de 200 caracteres")
        String titulo,

        @Size(max = 2000, message = "não pode ter mais de 2000 caracteres")
        String descricao) {
}

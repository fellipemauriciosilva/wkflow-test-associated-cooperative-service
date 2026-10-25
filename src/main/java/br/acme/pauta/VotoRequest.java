package br.acme.pauta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VotoRequest(
        @NotBlank(message = "obrigatorio")
        @Size(max = Voto.TAMANHO_ASSOCIADO, message = "deve ter no maximo {max} caracteres")
        String associadoId,

        @NotNull(message = "obrigatorio")
        @Pattern(regexp = "SIM|NAO", message = "deve ser SIM ou NAO")
        String opcao) {
}

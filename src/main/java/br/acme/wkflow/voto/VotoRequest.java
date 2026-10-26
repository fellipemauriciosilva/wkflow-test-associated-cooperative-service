package br.acme.wkflow.voto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VotoRequest(
        @NotBlank
        @Size(max = 255)
        @Pattern(regexp = "^[\\x20[^\\p{Cc}\\p{Cf}\\p{Zs}\\p{Zl}\\p{Zp}]]*$")
        String associadoId,
        String valor) {

    public VotoRequest {
        if (associadoId != null) {
            int ini = 0;
            int fim = associadoId.length();
            while (ini < fim && associadoId.charAt(ini) == ' ') {
                ini++;
            }
            while (fim > ini && associadoId.charAt(fim - 1) == ' ') {
                fim--;
            }
            associadoId = associadoId.substring(ini, fim);
        }
    }
}

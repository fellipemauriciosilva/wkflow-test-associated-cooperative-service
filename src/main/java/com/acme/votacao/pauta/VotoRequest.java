package com.acme.votacao.pauta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * {@code associadoId} é normalizado (trim + lowercase) neste construtor compacto, não no
 * serviço: assim o {@code @Size} valida o valor já normalizado. Normalizar depois da
 * validação faria um valor bruto de 256 caracteres com espaços nas bordas (254 úteis) ser
 * rejeitado por engano, e um valor com caractere que expande ao virar minúsculo (ex.
 * {@code İ}, U+0130) passar da validação e só estourar o limite da coluna no banco.
 */
public record VotoRequest(
        @NotBlank(message = "associadoId é obrigatório")
        @Size(max = 255, message = "associadoId deve ter no máximo 255 caracteres")
        String associadoId,
        @NotNull(message = "voto é obrigatório")
        VotoValor voto
) {

    public VotoRequest {
        associadoId = associadoId == null ? null : associadoId.trim().toLowerCase(Locale.ROOT);
    }
}

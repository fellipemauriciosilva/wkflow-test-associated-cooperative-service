package pautas;

import jakarta.validation.constraints.Min;

public record AbrirSessaoRequest(@Min(value = 1, message = "deve ser um inteiro maior que zero") Integer duracaoMinutos) {
}

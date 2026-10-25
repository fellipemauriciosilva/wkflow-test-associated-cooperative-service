package br.acme.pauta;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record SessaoRequest(
        @Digits(integer = 9, fraction = 0, message = "deve ser um numero inteiro")
        @Min(value = 1, message = "deve ser maior que zero")
        BigDecimal duracaoMinutos) {

    static final int DURACAO_PADRAO_MINUTOS = 1;

    int duracaoEmMinutos() {
        return duracaoMinutos == null ? DURACAO_PADRAO_MINUTOS : duracaoMinutos.intValueExact();
    }
}

package com.acme.votacao.pauta;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller existente só no classpath de teste (component-scan detecta por estar no mesmo
 * pacote de {@code com.acme.votacao}), usado para exercitar via HTTP real exceções do próprio
 * Spring MVC (parâmetro obrigatório ausente) que {@link GlobalExceptionHandler} precisa tratar
 * sem convertê-las em 500 — sem adicionar parâmetro novo a nenhum endpoint de produção.
 */
@RestController
public class DiagnosticoTestController {

    @GetMapping("/diagnostico")
    public Map<String, String> diagnostico(@RequestParam String x) {
        return Map.of("x", x);
    }
}

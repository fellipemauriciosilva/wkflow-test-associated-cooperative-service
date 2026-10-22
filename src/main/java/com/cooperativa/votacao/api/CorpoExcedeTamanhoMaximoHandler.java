package com.cooperativa.votacao.api;

import com.cooperativa.votacao.api.pauta.dto.ErroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global (nao escopado), porque TamanhoMaximoBodyFilter.CorpoExcedeTamanhoMaximoException e
 * lancada durante a leitura do body (dentro de DispatcherServlet.doDispatch, via Jackson),
 * nao dentro de um metodo de controller — precisa interceptar antes que
 * FrameworkServlet.processRequest embrulhe qualquer Throwable nao tratado em
 * ServletException, o que faria o filtro (que so captura o tipo original) nunca pegar e a
 * resposta virar 500 em vez de 413 (H1 do review de seguranca). Resolvendo aqui, dentro do
 * ciclo de doDispatch, a exececao nunca chega a escapar ate FrameworkServlet.
 *
 * O catch em TamanhoMaximoBodyFilter.doFilter fica como defesa residual (nao MVC, ex.
 * leitura de body fora do fluxo do DispatcherServlet), mas no caminho real de
 * POST /pautas e POST /pautas/{id}/sessao e este handler que responde.
 *
 * O desembrulho de HttpMessageNotReadableException fica SO AQUI (unico lugar), nao em
 * handlers escopados por controller: Jackson embrulha QUALQUER exececao do InputStream
 * (incluindo CorpoExcedeTamanhoMaximoException) nesse tipo, e um @RestControllerAdvice
 * escopado (assignableTypes) tem precedencia sobre o global — se um handler escopado
 * tratasse HttpMessageNotReadableException como 400 sem desembrulhar, o 413 nunca
 * chegaria aqui pra essa rota.
 */
@RestControllerAdvice
public class CorpoExcedeTamanhoMaximoHandler {

    @ExceptionHandler(TamanhoMaximoBodyFilter.CorpoExcedeTamanhoMaximoException.class)
    public ResponseEntity<ErroResponse> tratarCorpoExcedeTamanhoMaximo(
            TamanhoMaximoBodyFilter.CorpoExcedeTamanhoMaximoException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErroResponse(HttpStatus.PAYLOAD_TOO_LARGE.value(),
                        "corpo da requisicao excede o tamanho maximo permitido"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarJsonInvalido(HttpMessageNotReadableException ex) {
        if (causaEhExcederLimiteDeBody(ex)) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(new ErroResponse(HttpStatus.PAYLOAD_TOO_LARGE.value(),
                            "corpo da requisicao excede o tamanho maximo permitido"));
        }
        return ResponseEntity.badRequest()
                .body(new ErroResponse(HttpStatus.BAD_REQUEST.value(), "corpo da requisicao invalido ou ausente"));
    }

    private static boolean causaEhExcederLimiteDeBody(Throwable ex) {
        for (var causa = ex; causa != null; causa = causa.getCause()) {
            if (causa instanceof TamanhoMaximoBodyFilter.CorpoExcedeTamanhoMaximoException) {
                return true;
            }
        }
        return false;
    }
}

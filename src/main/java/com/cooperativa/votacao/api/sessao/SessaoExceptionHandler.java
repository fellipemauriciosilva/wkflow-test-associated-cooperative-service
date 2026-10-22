package com.cooperativa.votacao.api.sessao;

import com.cooperativa.votacao.api.pauta.dto.ErroResponse;
import com.cooperativa.votacao.domain.pauta.PautaNaoEncontradaException;
import com.cooperativa.votacao.domain.sessao.SessaoInvalidaException;
import com.cooperativa.votacao.domain.sessao.SessaoJaAbertaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = SessaoController.class)
public class SessaoExceptionHandler {

    @ExceptionHandler(SessaoInvalidaException.class)
    public ResponseEntity<ErroResponse> tratarSessaoInvalida(SessaoInvalidaException ex) {
        return ResponseEntity.badRequest()
                .body(new ErroResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(PautaNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> tratarPautaNaoEncontrada(PautaNaoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(SessaoJaAbertaException.class)
    public ResponseEntity<ErroResponse> tratarSessaoJaAberta(SessaoJaAbertaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroResponse(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    // Duracao nao inteira (ex. "abc", 1.5) falha na desserializacao do Integer pelo Jackson,
    // antes de chegar ao service — chega aqui como HttpMessageNotReadableException, nao como
    // SessaoInvalidaException.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarJsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new ErroResponse(HttpStatus.BAD_REQUEST.value(), "corpo da requisicao invalido"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(new ErroResponse(HttpStatus.BAD_REQUEST.value(), "parametro '" + ex.getName() + "' invalido"));
    }
}

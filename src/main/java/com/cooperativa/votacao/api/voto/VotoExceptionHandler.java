package com.cooperativa.votacao.api.voto;

import com.cooperativa.votacao.api.pauta.dto.ErroResponse;
import com.cooperativa.votacao.domain.pauta.PautaNaoEncontradaException;
import com.cooperativa.votacao.domain.voto.SessaoNaoAbertaException;
import com.cooperativa.votacao.domain.voto.VotoDuplicadoException;
import com.cooperativa.votacao.domain.voto.VotoInvalidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = VotoController.class)
public class VotoExceptionHandler {

    @ExceptionHandler(VotoInvalidoException.class)
    public ResponseEntity<ErroResponse> tratarVotoInvalido(VotoInvalidoException ex) {
        return ResponseEntity.badRequest()
                .body(new ErroResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(PautaNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> tratarPautaNaoEncontrada(PautaNaoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(SessaoNaoAbertaException.class)
    public ResponseEntity<ErroResponse> tratarSessaoNaoAberta(SessaoNaoAbertaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroResponse(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    @ExceptionHandler(VotoDuplicadoException.class)
    public ResponseEntity<ErroResponse> tratarVotoDuplicado(VotoDuplicadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroResponse(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    // HttpMessageNotReadableException (JSON malformado, enum invalido tipo "TALVEZ", ou 413
    // disfarcado) tratada SO no handler global (ver CorpoExcedeTamanhoMaximoHandler), nao
    // aqui — mesma razao de PautaExceptionHandler/SessaoExceptionHandler: handler escopado
    // tem precedencia sobre o global, e sem desembrulhar CorpoExcedeTamanhoMaximoException,
    // 413 por body grande nesta rota viraria 400.

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(new ErroResponse(HttpStatus.BAD_REQUEST.value(), "parametro '" + ex.getName() + "' invalido"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> tratarContentTypeNaoSuportado(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErroResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "content-type nao suportado"));
    }
}

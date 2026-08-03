package com.cooperative.associated.infrastructure.rest;

import com.cooperative.associated.application.exception.AssociateNotFoundException;
import com.cooperative.associated.application.exception.DuplicateDocumentException;
import com.cooperative.associated.domain.exception.InvalidAssociateDataException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InvalidAssociateDataException.class)
    public ResponseEntity<Object> handleInvalidData(InvalidAssociateDataException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid associate data");
        problem.setType(URI.create("https://api.cooperative.com/problems/invalid-associate-data"));
        problem.setInstance(URI.create(requestPath(request)));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(AssociateNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(AssociateNotFoundException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Associate not found");
        problem.setType(URI.create("https://api.cooperative.com/problems/associate-not-found"));
        problem.setInstance(URI.create(requestPath(request)));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(DuplicateDocumentException.class)
    public ResponseEntity<Object> handleDuplicateDocument(DuplicateDocumentException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate document");
        problem.setType(URI.create("https://api.cooperative.com/problems/duplicate-document"));
        problem.setInstance(URI.create(requestPath(request)));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        StringBuilder detail = new StringBuilder();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                detail.append(fieldError.getField()).append(": ").append(fieldError.getDefaultMessage()).append("; "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail.toString().trim());
        problem.setTitle("Validation failed");
        problem.setType(URI.create("https://api.cooperative.com/problems/validation-error"));
        problem.setInstance(URI.create(requestPath(request)));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    private String requestPath(WebRequest request) {
        String description = request.getDescription(false);
        return description.replace("uri=", "");
    }
}

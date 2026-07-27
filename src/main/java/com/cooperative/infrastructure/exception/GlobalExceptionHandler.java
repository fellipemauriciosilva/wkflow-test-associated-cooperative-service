package com.cooperative.infrastructure.exception;

import com.cooperative.domain.exception.DomainException;
import com.cooperative.domain.exception.DuplicateDocumentException;
import com.cooperative.domain.exception.EntityNotFoundException;
import com.cooperative.domain.exception.InvalidAssemblyException;
import com.cooperative.domain.exception.InvalidAssociateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import java.net.URI;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST controllers.
 * Converts domain exceptions to RFC 7807 ProblemDetail responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DuplicateDocumentException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateDocument(
            DuplicateDocumentException ex, WebRequest request) {
        logger.warn("Duplicate document error: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        problem.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFound(
            EntityNotFoundException ex, WebRequest request) {
        logger.warn("Entity not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Not Found");
        problem.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(InvalidAssociateException.class)
    public ResponseEntity<ProblemDetail> handleInvalidAssociate(
            InvalidAssociateException ex, WebRequest request) {
        logger.warn("Invalid associate: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Bad Request");
        problem.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(InvalidAssemblyException.class)
    public ResponseEntity<ProblemDetail> handleInvalidAssembly(
            InvalidAssemblyException ex, WebRequest request) {
        logger.warn("Invalid assembly: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Bad Request");
        problem.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationError(
            MethodArgumentNotValidException ex, WebRequest request) {
        String details = ex.getBindingResult()
            .getAllErrors()
            .stream()
            .map(error -> error.getDefaultMessage())
            .collect(Collectors.joining("; "));
        logger.warn("Validation error: {}", details);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, details);
        problem.setTitle("Validation Failed");
        problem.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex, WebRequest request) {
        logger.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}

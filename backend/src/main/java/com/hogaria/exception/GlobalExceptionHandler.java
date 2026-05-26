package com.hogaria.exception;

import com.hogaria.integration.cjprestamos.CjPrestamosIntegrationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class})
    ResponseEntity<ErrorResponse> bad(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, "BAD_REQUEST", List.of());
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(NotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, "NOT_FOUND", List.of());
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ErrorResponse> forbidden(ForbiddenException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req, "FORBIDDEN", List.of());
    }

    @ExceptionHandler({ConflictException.class})
    ResponseEntity<ErrorResponse> conflict(ConflictException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, "CONFLICT", List.of());
    }

    @ExceptionHandler(DomainConflictException.class)
    ResponseEntity<ErrorResponse> domainConflict(DomainConflictException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, ex.getCode(), ex.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.Detail(error.getField(), error.getDefaultMessage()))
                .toList();

        return build(
                HttpStatus.BAD_REQUEST,
                "La solicitud tiene campos inválidos.",
                req,
                "VALIDATION_ERROR",
                details
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> constraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        var details = ex.getConstraintViolations()
                .stream()
                .map(violation -> new ErrorResponse.Detail(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();

        return build(
                HttpStatus.BAD_REQUEST,
                "La solicitud viola restricciones de validación.",
                req,
                "CONSTRAINT_VIOLATION",
                details
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(
                HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no se pudo leer.",
                req,
                "MALFORMED_REQUEST",
                List.of()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> dataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn(
                "Data integrity conflict on {} {}: {}",
                req.getMethod(),
                req.getRequestURI(),
                rootMessage(ex)
        );

        return build(
                HttpStatus.CONFLICT,
                "La operación no puede completarse porque existen datos relacionados.",
                req,
                "DATA_INTEGRITY_CONFLICT",
                List.of(new ErrorResponse.Detail("constraint", rootMessage(ex)))
        );
    }

    @ExceptionHandler(CjPrestamosIntegrationException.class)
    ResponseEntity<ErrorResponse> cjPrestamos(CjPrestamosIntegrationException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage(), req, "CJPRESTAMOS_INTEGRATION_ERROR", List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected backend error on {} {}", req.getMethod(), req.getRequestURI(), ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor.",
                req,
                "INTERNAL_ERROR",
                List.of()
        );
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest req,
            String code,
            List<ErrorResponse.Detail> details
    ) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(
                        OffsetDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        req.getRequestURI(),
                        code,
                        details == null ? List.of() : details
                ));
    }

    private String rootMessage(Throwable ex) {
        var current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}

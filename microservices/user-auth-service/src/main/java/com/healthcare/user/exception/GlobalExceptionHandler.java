package com.healthcare.user.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(KeycloakResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(KeycloakResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Keycloak resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiErrorResponse.builder()
                        .status(HttpStatus.NOT_FOUND.value())
                        .error("Not Found")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .timestamp(ZonedDateTime.now().toString())
                        .build()
        );
    }

    @ExceptionHandler(KeycloakResourceConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(KeycloakResourceConflictException ex, HttpServletRequest request) {
        log.warn("Keycloak resource conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiErrorResponse.builder()
                        .status(HttpStatus.CONFLICT.value())
                        .error("Conflict")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .timestamp(ZonedDateTime.now().toString())
                        .build()
        );
    }

    @ExceptionHandler(KeycloakAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(KeycloakAccessDeniedException ex, HttpServletRequest request) {
        log.warn("Keycloak access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiErrorResponse.builder()
                        .status(HttpStatus.FORBIDDEN.value())
                        .error("Forbidden")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .timestamp(ZonedDateTime.now().toString())
                        .build()
        );
    }

    @ExceptionHandler(KeycloakOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleOperationException(KeycloakOperationException ex, HttpServletRequest request) {
        log.error("Keycloak operation failed: {}", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(
                ApiErrorResponse.builder()
                        .status(ex.getStatus().value())
                        .error(ex.getStatus().getReasonPhrase())
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .timestamp(ZonedDateTime.now().toString())
                        .build()
        );
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleKeycloakSdkNotFound(NotFoundException ex, HttpServletRequest request) {
        log.warn("Keycloak SDK 404: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiErrorResponse.builder()
                        .status(HttpStatus.NOT_FOUND.value())
                        .error("Not Found")
                        .message("The requested Keycloak resource (role, user, or client) does not exist.")
                        .path(request.getRequestURI())
                        .timestamp(ZonedDateTime.now().toString())
                        .build()
        );
    }

    @ExceptionHandler(WebApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleKeycloakWebApplicationException(WebApplicationException ex, HttpServletRequest request) {
        int statusCode = ex.getResponse().getStatus();
        HttpStatus status = HttpStatus.resolve(statusCode);
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

        log.error("Keycloak SDK error (HTTP {}): {}", statusCode, ex.getMessage());

        String message = "Keycloak IAM operation failed with status " + statusCode;
        if (statusCode == 409) message = "A resource with the specified identifier or name already exists in Keycloak.";
        else if (statusCode == 403) message = "Insufficient permissions to execute Keycloak admin operation.";
        else if (statusCode == 404) message = "Keycloak resource not found.";

        return ResponseEntity.status(status).body(
                ApiErrorResponse.builder()
                        .status(statusCode)
                        .error(status.getReasonPhrase())
                        .message(message)
                        .path(request.getRequestURI())
                        .timestamp(ZonedDateTime.now().toString())
                        .build()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("Bad Request")
                        .message("Request payload validation failed.")
                        .validationErrors(errors)
                        .path(request.getRequestURI())
                        .timestamp(ZonedDateTime.now().toString())
                        .build()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("Bad Request")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .timestamp(ZonedDateTime.now().toString())
                        .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unhandled server exception: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiErrorResponse.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .error("Internal Server Error")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .timestamp(ZonedDateTime.now().toString())
                        .build()
        );
    }
}

package com.healthcare.user.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class KeycloakOperationException extends RuntimeException {
    private final HttpStatus status;

    public KeycloakOperationException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public KeycloakOperationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public KeycloakOperationException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }
}

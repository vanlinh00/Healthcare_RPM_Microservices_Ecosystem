package com.healthcare.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class KeycloakResourceConflictException extends RuntimeException {
    public KeycloakResourceConflictException(String message) {
        super(message);
    }
}

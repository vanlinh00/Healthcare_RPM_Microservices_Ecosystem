package com.healthcare.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class KeycloakAccessDeniedException extends RuntimeException {
    public KeycloakAccessDeniedException(String message) {
        super(message);
    }
}

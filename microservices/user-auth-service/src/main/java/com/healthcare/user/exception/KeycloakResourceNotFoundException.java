package com.healthcare.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class KeycloakResourceNotFoundException extends RuntimeException {
    public KeycloakResourceNotFoundException(String message) {
        super(message);
    }
}

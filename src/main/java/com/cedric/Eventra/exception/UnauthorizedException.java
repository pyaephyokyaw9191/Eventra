package com.cedric.Eventra.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN) // This tells Spring to return a 403 status if unhandled
// Or HttpStatus.UNAUTHORIZED (401) if it's about missing authentication vs. permissions
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}

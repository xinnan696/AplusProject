package com.ucd.urbanflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for handling cases where a resource (e.g., a user with a specific email)
 * already exists when trying to create a new one.
 * Maps to HTTP 409 Conflict status.
 */
@ResponseStatus(HttpStatus.CONFLICT) // This annotation helps Spring map this exception to a 409 status code
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
package io.github.lvoxx.common_core.exception.model.user_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.DuplicateResourceException;

/**
 * Exception thrown when a user already exists
 */
public class UserAlreadyExistsException extends DuplicateResourceException {

    public UserAlreadyExistsException() {
        super();
    }

    public UserAlreadyExistsException(String message) {
        super(message);
    }

    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
package io.github.lvoxx.common_core.exception.model;

/**
 * Exception thrown when a duplicate resource is attempted to be created
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException() {
        super();
    }

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
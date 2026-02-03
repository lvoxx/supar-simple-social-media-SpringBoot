package io.github.lvoxx.common_core.exception.model;

/**
 * Exception thrown when unknown errors occur
 */
public class UnknownException extends RuntimeException {

    public UnknownException() {
        super();
    }

    public UnknownException(String message) {
        super(message);
    }

    public UnknownException(String message, Throwable cause) {
        super(message, cause);
    }
}
package io.github.lvoxx.common_core.exception.model.common_exception;

import io.github.lvoxx.common_core.exception.model.ApplicationException;

/**
 * Exception thrown when an invalid operation is attempted
 */
public class InvalidException extends ApplicationException {

    public InvalidException() {
        super();
    }

    public InvalidException(String message) {
        super(message);
    }

    public InvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
package io.github.lvoxx.common_core.exception.model.common_exception;

import io.github.lvoxx.common_core.exception.model.ApplicationException;

/**
 * Exception thrown when a timeout occurs
 */
public class TimeoutException extends ApplicationException {

    public TimeoutException() {
        super();
    }

    public TimeoutException(String message) {
        super(message);
    }

    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
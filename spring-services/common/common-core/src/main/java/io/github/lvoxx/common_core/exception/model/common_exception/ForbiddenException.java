package io.github.lvoxx.common_core.exception.model.common_exception;

import io.github.lvoxx.common_core.exception.model.ApplicationException;

/**
 * Exception thrown when an action is forbidden
 */
public class ForbiddenException extends ApplicationException {

    public ForbiddenException() {
        super();
    }

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
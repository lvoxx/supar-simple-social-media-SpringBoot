package io.github.lvoxx.common_core.exception.model.common_exception;

import io.github.lvoxx.common_core.exception.model.ApplicationException;

/**
 * Exception thrown when a resource is blocked
 */
public class ResourceBlockedException extends ApplicationException {

    public ResourceBlockedException() {
        super();
    }

    public ResourceBlockedException(String message) {
        super(message);
    }

    public ResourceBlockedException(String message, Throwable cause) {
        super(message, cause);
    }
}
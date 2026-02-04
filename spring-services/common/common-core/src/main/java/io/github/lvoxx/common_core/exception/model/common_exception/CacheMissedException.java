package io.github.lvoxx.common_core.exception.model.common_exception;

import io.github.lvoxx.common_core.exception.model.ApplicationException;

/**
 * Exception thrown when a cache miss occurs
 */
public class CacheMissedException extends ApplicationException {

    public CacheMissedException() {
        super();
    }

    public CacheMissedException(String message) {
        super(message);
    }

    public CacheMissedException(String message, Throwable cause) {
        super(message, cause);
    }
}
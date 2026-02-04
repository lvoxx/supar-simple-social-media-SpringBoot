package io.github.lvoxx.common_core.exception.model.post_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.ResourceNotFoundException;

/**
 * Exception thrown when content violates policies
 */
public class ContentViolationException extends ResourceNotFoundException {

    public ContentViolationException() {
        super();
    }

    public ContentViolationException(String message) {
        super(message);
    }

    public ContentViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
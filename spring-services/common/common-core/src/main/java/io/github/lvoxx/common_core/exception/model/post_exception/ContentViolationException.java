package io.github.lvoxx.common_core.exception.model.post_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.ResourceBlockedException;

/**
 * Exception thrown when content violates policies
 */
public class ContentViolationException extends ResourceBlockedException {

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
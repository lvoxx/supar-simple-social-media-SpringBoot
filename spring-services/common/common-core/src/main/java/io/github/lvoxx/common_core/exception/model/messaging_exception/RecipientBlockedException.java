package io.github.lvoxx.common_core.exception.model.messaging_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.ResourceBlockedException;

/**
 * Exception thrown when a message send fails
 */
public class RecipientBlockedException extends ResourceBlockedException {

    public RecipientBlockedException() {
        super();
    }

    public RecipientBlockedException(String message) {
        super(message);
    }

    public RecipientBlockedException(String message, Throwable cause) {
        super(message, cause);
    }
}
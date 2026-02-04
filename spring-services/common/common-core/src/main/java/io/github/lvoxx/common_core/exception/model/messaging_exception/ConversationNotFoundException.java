package io.github.lvoxx.common_core.exception.model.messaging_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.ResourceNotFoundException;

/**
 * Exception thrown when a conversation is not found
 */
public class ConversationNotFoundException extends ResourceNotFoundException {

    public ConversationNotFoundException() {
        super();
    }

    public ConversationNotFoundException(String message) {
        super(message);
    }

    public ConversationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
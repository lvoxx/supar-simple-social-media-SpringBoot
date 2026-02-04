package io.github.lvoxx.common_core.exception.model.messaging_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.ConcurrencyException;

/**
 * Exception thrown when a message send fails
 */
public class MessageSendFailedException extends ConcurrencyException {

    public MessageSendFailedException() {
        super();
    }

    public MessageSendFailedException(String message) {
        super(message);
    }

    public MessageSendFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
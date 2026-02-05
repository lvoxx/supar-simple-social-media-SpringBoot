package io.github.lvoxx.common_core.exception.model.cs_service;

import io.github.lvoxx.common_core.exception.model.common_exception.InvalidException;

/**
 * Indicates that a ticket is already closed.
 */
public class TicketAlreadyClosedException extends InvalidException {

    public TicketAlreadyClosedException() {
        super();
    }

    public TicketAlreadyClosedException(String message) {
        super(message);
    }

    public TicketAlreadyClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}
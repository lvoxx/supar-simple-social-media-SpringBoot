package io.github.lvoxx.common_core.exception.model.cs_service;

import io.github.lvoxx.common_core.exception.model.common_exception.ResourceNotFoundException;

/**
 * Indicates that a ticket was not found.
 */
public class TicketNotFoundException extends ResourceNotFoundException {

    public TicketNotFoundException() {
        super();
    }

    public TicketNotFoundException(String message) {
        super(message);
    }

    public TicketNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
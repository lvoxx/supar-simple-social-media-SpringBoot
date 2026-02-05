package io.github.lvoxx.common_core.exception.model.analytics_ingest;

import io.github.lvoxx.common_core.exception.model.common_exception.ValidationException;

/**
 * Exception thrown when event validation fails
 */
public class EventValidationException extends ValidationException {

    public EventValidationException() {
        super();
    }

    public EventValidationException(String message) {
        super(message);
    }

    public EventValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
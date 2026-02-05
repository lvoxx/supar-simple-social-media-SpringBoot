package io.github.lvoxx.common_core.exception.model.analytics_ingest;

import io.github.lvoxx.common_core.exception.model.common_exception.ValidationException;

/**
 * Exception thrown when event schema validation fails
 */
public class EventSchemaMismatchException extends ValidationException {

    public EventSchemaMismatchException() {
        super();
    }

    public EventSchemaMismatchException(String message) {
        super(message);
    }

    public EventSchemaMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
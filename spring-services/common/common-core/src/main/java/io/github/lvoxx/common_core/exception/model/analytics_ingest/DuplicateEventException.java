package io.github.lvoxx.common_core.exception.model.analytics_ingest;

import io.github.lvoxx.common_core.exception.model.common_exception.DuplicateResourceException;

/**
 * Indicates that a duplicate event was detected during ingestion.
 */
public class DuplicateEventException extends DuplicateResourceException {

    public DuplicateEventException() {
        super();
    }

    public DuplicateEventException(String message) {
        super(message);
    }

    public DuplicateEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
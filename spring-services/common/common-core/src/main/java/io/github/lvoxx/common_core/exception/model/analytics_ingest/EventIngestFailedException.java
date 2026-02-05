package io.github.lvoxx.common_core.exception.model.analytics_ingest;

import io.github.lvoxx.common_core.exception.model.common_exception.ValidationException;

/**
 * Indicates that an event ingestion has failed due to validation issues.
 */
public class EventIngestFailedException extends ValidationException {

    public EventIngestFailedException() {
        super();
    }

    public EventIngestFailedException(String message) {
        super(message);
    }

    public EventIngestFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
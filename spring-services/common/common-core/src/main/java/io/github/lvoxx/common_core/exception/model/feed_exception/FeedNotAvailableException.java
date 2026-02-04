package io.github.lvoxx.common_core.exception.model.feed_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.InfrastructureException;

/**
 * Exception thrown when a feed is not available
 */
public class FeedNotAvailableException extends InfrastructureException {

    public FeedNotAvailableException() {
        super();
    }

    public FeedNotAvailableException(String message) {
        super(message);
    }

    public FeedNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
package io.github.lvoxx.common_core.exception.model.feed_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.ResourceNotFoundException;

/**
 * Exception thrown when a feed generation fails
 */
public class FeedGenerationFailedException extends ResourceNotFoundException {

    public FeedGenerationFailedException() {
        super();
    }

    public FeedGenerationFailedException(String message) {
        super(message);
    }

    public FeedGenerationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
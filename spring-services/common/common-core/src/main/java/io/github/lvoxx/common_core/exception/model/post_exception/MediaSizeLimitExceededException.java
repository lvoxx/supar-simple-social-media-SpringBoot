package io.github.lvoxx.common_core.exception.model.post_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.ResourceNotFoundException;

/**
 * Exception thrown when media size exceeds the limit
 */
public class MediaSizeLimitExceededException extends ResourceNotFoundException {

    public MediaSizeLimitExceededException() {
        super();
    }

    public MediaSizeLimitExceededException(String message) {
        super(message);
    }

    public MediaSizeLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
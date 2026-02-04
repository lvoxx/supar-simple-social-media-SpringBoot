package io.github.lvoxx.common_core.exception.model.post_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.ResourceNotFoundException;

/**
 * Exception thrown when a post has been deleted
 */
public class PostDeletedException extends ResourceNotFoundException {

    public PostDeletedException() {
        super();
    }

    public PostDeletedException(String message) {
        super(message);
    }

    public PostDeletedException(String message, Throwable cause) {
        super(message, cause);
    }
}
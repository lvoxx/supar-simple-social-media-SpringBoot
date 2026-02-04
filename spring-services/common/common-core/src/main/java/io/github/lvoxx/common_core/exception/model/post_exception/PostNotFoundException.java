package io.github.lvoxx.common_core.exception.model.post_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.ResourceNotFoundException;

/**
 * Exception thrown when a post is not found
 */
public class PostNotFoundException extends ResourceNotFoundException {

    public PostNotFoundException() {
        super();
    }

    public PostNotFoundException(String message) {
        super(message);
    }

    public PostNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
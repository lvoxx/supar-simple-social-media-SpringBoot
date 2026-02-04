package io.github.lvoxx.common_core.exception.model.post_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.ResourceNotFoundException;

/**
 * Exception thrown when a comment is not found
 */
public class CommentNotFoundException extends ResourceNotFoundException {

    public CommentNotFoundException() {
        super();
    }

    public CommentNotFoundException(String message) {
        super(message);
    }

    public CommentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
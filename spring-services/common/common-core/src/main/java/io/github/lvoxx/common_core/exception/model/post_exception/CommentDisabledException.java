package io.github.lvoxx.common_core.exception.model.post_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.ResourceBlockedException;

/**
 * Exception thrown when a comment is disabled
 */
public class CommentDisabledException extends ResourceBlockedException {

    public CommentDisabledException() {
        super();
    }

    public CommentDisabledException(String message) {
        super(message);
    }

    public CommentDisabledException(String message, Throwable cause) {
        super(message, cause);
    }
}
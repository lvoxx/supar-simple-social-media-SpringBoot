package io.github.lvoxx.common_core.exception.model.user_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.ResourceBlockedException;

/**
 * Exception thrown when a user is blocked
 */
public class UserBlockedException extends ResourceBlockedException {

    public UserBlockedException() {
        super();
    }

    public UserBlockedException(String message) {
        super(message);
    }

    public UserBlockedException(String message, Throwable cause) {
        super(message, cause);
    }
}
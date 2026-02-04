package io.github.lvoxx.common_core.exception.model.user_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.ResourceBlockedException;

/**
 * Exception thrown when a user is deactivated
 */
public class UserDeactivatedException extends ResourceBlockedException {

    public UserDeactivatedException() {
        super();
    }

    public UserDeactivatedException(String message) {
        super(message);
    }

    public UserDeactivatedException(String message, Throwable cause) {
        super(message, cause);
    }
}
package io.github.lvoxx.common_core.exception.model.user_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.ForbiddenException;

/**
 * Exception thrown when profile update is not allowed
 */
public class ProfileUpdateNotAllowedException extends ForbiddenException {

    public ProfileUpdateNotAllowedException() {
        super();
    }

    public ProfileUpdateNotAllowedException(String message) {
        super(message);
    }

    public ProfileUpdateNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }
}
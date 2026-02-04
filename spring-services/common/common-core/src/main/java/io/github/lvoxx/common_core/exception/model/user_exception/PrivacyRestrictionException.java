package io.github.lvoxx.common_core.exception.model.user_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.ForbiddenException;

/**
 * Exception thrown when privacy restrictions prevent an action
 */
public class PrivacyRestrictionException extends ForbiddenException {

    public PrivacyRestrictionException() {
        super();
    }

    public PrivacyRestrictionException(String message) {
        super(message);
    }

    public PrivacyRestrictionException(String message, Throwable cause) {
        super(message, cause);
    }
}
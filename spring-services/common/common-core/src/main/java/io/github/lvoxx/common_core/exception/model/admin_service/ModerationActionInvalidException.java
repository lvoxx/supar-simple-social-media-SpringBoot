package io.github.lvoxx.common_core.exception.model.admin_service;

import io.github.lvoxx.common_core.exception.model.common_exception.InvalidException;

/**
 * Indicates that a moderation action is invalid or not permitted.
 */
public class ModerationActionInvalidException extends InvalidException {

    public ModerationActionInvalidException() {
        super();
    }

    public ModerationActionInvalidException(String message) {
        super(message);
    }

    public ModerationActionInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
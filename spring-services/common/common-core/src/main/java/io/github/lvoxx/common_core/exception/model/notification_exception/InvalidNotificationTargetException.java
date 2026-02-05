package io.github.lvoxx.common_core.exception.model.notification_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.InvalidException;

/**
 * Exception thrown when notification delivery fails
 */
public class InvalidNotificationTargetException extends InvalidException {

    public InvalidNotificationTargetException() {
        super();
    }

    public InvalidNotificationTargetException(String message) {
        super(message);
    }

    public InvalidNotificationTargetException(String message, Throwable cause) {
        super(message, cause);
    }
}
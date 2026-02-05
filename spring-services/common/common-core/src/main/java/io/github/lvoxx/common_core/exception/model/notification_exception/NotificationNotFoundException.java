package io.github.lvoxx.common_core.exception.model.notification_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.ResourceNotFoundException;

/**
 * Exception thrown when a notification is not found
 */
public class NotificationNotFoundException extends ResourceNotFoundException {

    public NotificationNotFoundException() {
        super();
    }

    public NotificationNotFoundException(String message) {
        super(message);
    }

    public NotificationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
package io.github.lvoxx.common_core.exception.model.notification_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.ConcurrencyException;

/**
 * Exception thrown when notification delivery fails
 */
public class NotificationDeliveryFailedException extends ConcurrencyException {

    public NotificationDeliveryFailedException() {
        super();
    }

    public NotificationDeliveryFailedException(String message) {
        super(message);
    }

    public NotificationDeliveryFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
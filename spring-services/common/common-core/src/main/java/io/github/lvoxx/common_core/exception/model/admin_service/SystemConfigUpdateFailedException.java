package io.github.lvoxx.common_core.exception.model.admin_service;

import io.github.lvoxx.common_core.exception.model.common_exception.InvalidException;

/**
 * Indicates that a system configuration update has failed.
 */
public class SystemConfigUpdateFailedException extends InvalidException {

    public SystemConfigUpdateFailedException() {
        super();
    }

    public SystemConfigUpdateFailedException(String message) {
        super(message);
    }

    public SystemConfigUpdateFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
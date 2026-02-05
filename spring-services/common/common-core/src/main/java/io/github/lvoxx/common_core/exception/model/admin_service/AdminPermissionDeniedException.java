package io.github.lvoxx.common_core.exception.model.admin_service;

import io.github.lvoxx.common_core.exception.model.common_exception.ForbiddenException;

/**
 * Indicates that an admin operation was denied due to insufficient permissions.
 */
public class AdminPermissionDeniedException extends ForbiddenException {

    public AdminPermissionDeniedException() {
        super();
    }

    public AdminPermissionDeniedException(String message) {
        super(message);
    }

    public AdminPermissionDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
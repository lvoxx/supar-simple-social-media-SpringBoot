package io.github.lvoxx.common_core.exception.model.cs_service;

import io.github.lvoxx.common_core.exception.model.common_exception.ForbiddenException;

/**
 * Indicates that an appeal is not allowed.
 */
public class AppealNotAllowedException extends ForbiddenException {

    public AppealNotAllowedException() {
        super();
    }

    public AppealNotAllowedException(String message) {
        super(message);
    }

    public AppealNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }
}
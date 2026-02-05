package io.github.lvoxx.common_core.exception.model.cs_service;

import io.github.lvoxx.common_core.exception.model.common_exception.InvalidException;

/**
 * Indicates that a report is invalid.
 */
public class ReportInvalidException extends InvalidException {

    public ReportInvalidException() {
        super();
    }

    public ReportInvalidException(String message) {
        super(message);
    }

    public ReportInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
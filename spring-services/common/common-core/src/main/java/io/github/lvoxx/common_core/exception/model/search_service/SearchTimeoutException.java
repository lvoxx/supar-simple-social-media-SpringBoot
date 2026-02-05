package io.github.lvoxx.common_core.exception.model.search_service;

import io.github.lvoxx.common_core.exception.model.common_exception.TimeoutException;

/**
 * Exception thrown when a search operation times out
 */
public class SearchTimeoutException extends TimeoutException {

    public SearchTimeoutException() {
        super();
    }

    public SearchTimeoutException(String message) {
        super(message);
    }

    public SearchTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
package io.github.lvoxx.common_core.exception.model.search_service;

import io.github.lvoxx.common_core.exception.model.common_exception.InvalidException;

/**
 * Exception thrown when validation fails
 */
public class SearchQueryInvalidException extends InvalidException {

    public SearchQueryInvalidException() {
        super();
    }

    public SearchQueryInvalidException(String message) {
        super(message);
    }

    public SearchQueryInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
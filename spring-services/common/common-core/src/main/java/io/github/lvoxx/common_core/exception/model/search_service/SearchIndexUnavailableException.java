package io.github.lvoxx.common_core.exception.model.search_service;

import io.github.lvoxx.common_core.exception.model.common_exception.InfrastructureException;

/**
 * Exception thrown when the search index is unavailable
 */
public class SearchIndexUnavailableException extends InfrastructureException {

    public SearchIndexUnavailableException() {
        super();
    }

    public SearchIndexUnavailableException(String message) {
        super(message);
    }

    public SearchIndexUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
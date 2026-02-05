package io.github.lvoxx.common_core.exception.model.search_service;

import io.github.lvoxx.common_core.exception.model.common_exception.RateLimitExceededException;

/**
 * Exception thrown when the search result limit is exceeded
 */
public class SearchResultLimitExceededException extends RateLimitExceededException {

    public SearchResultLimitExceededException() {
        super();
    }

    public SearchResultLimitExceededException(String message) {
        super(message);
    }

    public SearchResultLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
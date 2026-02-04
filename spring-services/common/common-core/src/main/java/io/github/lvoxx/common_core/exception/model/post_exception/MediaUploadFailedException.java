package io.github.lvoxx.common_core.exception.model.post_exception;

import io.github.lvoxx.common_core.exception.model.common_exception.InfrastructureException;

/**
 * Exception thrown when media upload fails
 */
public class MediaUploadFailedException extends InfrastructureException {

    public MediaUploadFailedException() {
        super();
    }

    public MediaUploadFailedException(String message) {
        super(message);
    }

    public MediaUploadFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
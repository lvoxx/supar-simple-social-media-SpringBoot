package io.github.lvoxx.common_core.exception;

public class ForbiddenException extends BusinessException {
    public ForbiddenException(String errorCode, Object... args) {
        super(errorCode, args);
    }
}

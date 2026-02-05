package io.github.lvoxx.common_core.exception.handler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

import io.github.lvoxx.common_core.exception.model.ApplicationException;
import io.github.lvoxx.common_core.exception.model.common_exception.*;
import io.github.lvoxx.common_core.exception.model.messaging_exception.*;
import io.github.lvoxx.common_core.exception.model.notification_exception.*;
import io.github.lvoxx.common_core.exception.model.post_exception.*;
import io.github.lvoxx.common_core.exception.model.search_service.*;
import io.github.lvoxx.common_core.exception.model.user_exception.*;
import io.github.lvoxx.common_core.exception.model.admin_service.*;
import io.github.lvoxx.common_core.exception.model.analytics_ingest.*;
import io.github.lvoxx.common_core.exception.model.cs_service.*;
import io.github.lvoxx.common_core.exception.model.feed_exception.*;
import io.github.lvoxx.common_core.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Global exception handler for reactive controllers
 * Handles all custom exceptions across all services
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== NOT_FOUND EXCEPTIONS (404) ====================
    // Common exceptions and service-specific exceptions with same HTTP status

    @ExceptionHandler({
            ResourceNotFoundException.class,
            CacheMissedException.class,
            PostNotFoundException.class,
            CommentNotFoundException.class,
            NotificationNotFoundException.class,
            ConversationNotFoundException.class,
            MessageNotFoundException.class,
            UserNotFoundException.class,
            TicketNotFoundException.class,
            ContentViolationException.class
    })
    public Mono<ResponseEntity<ErrorResponse>> handleNotFoundException(
            Exception ex, ServerWebExchange exchange) {
        log.error("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, exchange);
    }

    // ==================== CONFLICT EXCEPTIONS (409) ====================
    // Common exceptions and service-specific exceptions with same HTTP status

    @ExceptionHandler({
            DuplicateResourceException.class,
            ConcurrencyException.class,
            UserAlreadyExistsException.class,
            DuplicateEventException.class,
            TicketAlreadyClosedException.class
    })
    public Mono<ResponseEntity<ErrorResponse>> handleConflictException(
            Exception ex, ServerWebExchange exchange) {
        log.error("Conflict: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.CONFLICT, exchange);
    }

    // ==================== FORBIDDEN EXCEPTIONS (403) ====================
    // Common exceptions and service-specific exceptions with same HTTP status

    @ExceptionHandler({
            ForbiddenException.class,
            ResourceBlockedException.class,
            CommentDisabledException.class,
            RecipientBlockedException.class,
            PrivacyRestrictionException.class,
            ProfileUpdateNotAllowedException.class,
            UserBlockedException.class,
            UserDeactivatedException.class,
            AdminPermissionDeniedException.class,
            AppealNotAllowedException.class
    })
    public Mono<ResponseEntity<ErrorResponse>> handleForbiddenException(
            Exception ex, ServerWebExchange exchange) {
        log.warn("Forbidden access: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, exchange);
    }

    // ==================== BAD_REQUEST EXCEPTIONS (400) ====================
    // Common exceptions and service-specific exceptions with same HTTP status

    @ExceptionHandler({
            InvalidException.class,
            ValidationException.class,
            SearchQueryInvalidException.class,
            SearchResultLimitExceededException.class,
            ModerationActionInvalidException.class,
            EventSchemaMismatchException.class,
            EventValidationException.class,
            ReportInvalidException.class,
            InvalidNotificationTargetException.class
    })
    public Mono<ResponseEntity<ErrorResponse>> handleBadRequestException(
            Exception ex, ServerWebExchange exchange) {
        log.error("Invalid request: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, exchange);
    }

    // ==================== INTERNAL_SERVER_ERROR EXCEPTIONS (500)
    // ====================
    // Common exceptions and service-specific exceptions with same HTTP status

    @ExceptionHandler({
            SerializationException.class,
            MessageSendFailedException.class,
            NotificationDeliveryFailedException.class,
            MediaUploadFailedException.class,
            SystemConfigUpdateFailedException.class,
            EventIngestFailedException.class,
            FeedGenerationFailedException.class
    })
    public Mono<ResponseEntity<ErrorResponse>> handleInternalServerErrorException(
            Exception ex, ServerWebExchange exchange) {
        log.error("Internal server error: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, exchange);
    }

    // ==================== SERVICE_UNAVAILABLE EXCEPTIONS (503)
    // ====================
    // Common exceptions and service-specific exceptions with same HTTP status

    @ExceptionHandler({
            InfrastructureException.class,
            SearchIndexUnavailableException.class,
            FeedNotAvailableException.class
    })
    public Mono<ResponseEntity<ErrorResponse>> handleServiceUnavailableException(
            Exception ex, ServerWebExchange exchange) {
        log.error("Service unavailable: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, exchange);
    }

    // ==================== REQUEST_TIMEOUT EXCEPTIONS (408) ====================
    // Common exceptions and service-specific exceptions with same HTTP status

    @ExceptionHandler({
            TimeoutException.class,
            SearchTimeoutException.class
    })
    public Mono<ResponseEntity<ErrorResponse>> handleRequestTimeoutException(
            Exception ex, ServerWebExchange exchange) {
        log.error("Request timeout: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.REQUEST_TIMEOUT, exchange);
    }

    // ==================== TOO_MANY_REQUESTS EXCEPTION (429) ====================

    @ExceptionHandler(RateLimitExceededException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleRateLimitExceededException(
            RateLimitExceededException ex, ServerWebExchange exchange) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.TOO_MANY_REQUESTS, exchange);
    }

    // ==================== GONE EXCEPTION (410) ====================

    @ExceptionHandler(PostDeletedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handlePostDeletedException(
            PostDeletedException ex, ServerWebExchange exchange) {
        log.warn("Post deleted: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.GONE, exchange);
    }

    // ==================== CONTENT_TOO_LARGE EXCEPTION (413) ====================

    @ExceptionHandler(MediaSizeLimitExceededException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleMediaSizeLimitExceededException(
            MediaSizeLimitExceededException ex, ServerWebExchange exchange) {
        log.warn("Media size limit exceeded: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.CONTENT_TOO_LARGE, exchange);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleWebExchangeBindException(
            WebExchangeBindException ex, ServerWebExchange exchange) {
        log.error("Validation error: {}", ex.getMessage());

        List<String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.toList());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(exchange.getRequest().getPath().value())
                .details(errors)
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    // ==================== APPLICATION EXCEPTION ====================

    @ExceptionHandler(ApplicationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleApplicationException(
            ApplicationException ex, ServerWebExchange exchange) {
        log.error("Application error: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, exchange);
    }

    // ==================== GENERIC EXCEPTION ====================

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(
            Exception ex, ServerWebExchange exchange) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred")
                .path(exchange.getRequest().getPath().value())
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }

    // ==================== HELPER METHODS ====================

    /**
     * Build error response with consistent format
     */
    private Mono<ResponseEntity<ErrorResponse>> buildErrorResponse(
            Exception ex, HttpStatus status, ServerWebExchange exchange) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getMessage())
                .path(exchange.getRequest().getPath().value())
                .build();

        return Mono.just(ResponseEntity.status(status).body(error));
    }
}
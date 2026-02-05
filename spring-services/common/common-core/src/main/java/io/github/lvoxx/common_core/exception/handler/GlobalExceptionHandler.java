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

    // ==================== COMMON EXCEPTIONS ====================

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleResourceNotFoundException(
            ResourceNotFoundException ex, ServerWebExchange exchange) {
        log.error("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, exchange);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDuplicateResourceException(
            DuplicateResourceException ex, ServerWebExchange exchange) {
        log.error("Duplicate resource: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.CONFLICT, exchange);
    }

    @ExceptionHandler(CacheMissedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCacheMissedException(
            CacheMissedException ex, ServerWebExchange exchange) {
        log.warn("Cache missed: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, exchange);
    }

    @ExceptionHandler(ConcurrencyException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleConcurrencyException(
            ConcurrencyException ex, ServerWebExchange exchange) {
        log.error("Concurrency conflict: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.CONFLICT, exchange);
    }

    @ExceptionHandler(ForbiddenException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleForbiddenException(
            ForbiddenException ex, ServerWebExchange exchange) {
        log.warn("Forbidden access: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, exchange);
    }

    @ExceptionHandler(InfrastructureException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInfrastructureException(
            InfrastructureException ex, ServerWebExchange exchange) {
        log.error("Infrastructure error: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, exchange);
    }

    @ExceptionHandler(InvalidException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInvalidException(
            InvalidException ex, ServerWebExchange exchange) {
        log.error("Invalid request: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, exchange);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleRateLimitExceededException(
            RateLimitExceededException ex, ServerWebExchange exchange) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.TOO_MANY_REQUESTS, exchange);
    }

    @ExceptionHandler(ResourceBlockedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleResourceBlockedException(
            ResourceBlockedException ex, ServerWebExchange exchange) {
        log.warn("Resource blocked: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, exchange);
    }

    @ExceptionHandler(SerializationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleSerializationException(
            SerializationException ex, ServerWebExchange exchange) {
        log.error("Serialization error: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, exchange);
    }

    @ExceptionHandler(TimeoutException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleTimeoutException(
            TimeoutException ex, ServerWebExchange exchange) {
        log.error("Request timeout: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.REQUEST_TIMEOUT, exchange);
    }

    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(
            ValidationException ex, ServerWebExchange exchange) {
        log.error("Validation failed: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, exchange);
    }

    // ==================== MESSAGING EXCEPTIONS ====================

    @ExceptionHandler(ConversationNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleConversationNotFoundException(
            ConversationNotFoundException ex, ServerWebExchange exchange) {
        log.error("Conversation not found: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, exchange);
    }

    @ExceptionHandler(MessageNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleMessageNotFoundException(
            MessageNotFoundException ex, ServerWebExchange exchange) {
        log.error("Message not found: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, exchange);
    }

    @ExceptionHandler(MessageSendFailedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleMessageSendFailedException(
            MessageSendFailedException ex, ServerWebExchange exchange) {
        log.error("Message send failed: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, exchange);
    }

    @ExceptionHandler(RecipientBlockedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleRecipientBlockedException(
            RecipientBlockedException ex, ServerWebExchange exchange) {
        log.warn("Recipient blocked: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, exchange);
    }

    // ==================== NOTIFICATION EXCEPTIONS ====================

    @ExceptionHandler(InvalidNotificationTargetException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInvalidNotificationTargetException(
            InvalidNotificationTargetException ex, ServerWebExchange exchange) {
        log.error("Invalid notification target: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, exchange);
    }

    @ExceptionHandler(NotificationDeliveryFailedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleNotificationDeliveryFailedException(
            NotificationDeliveryFailedException ex, ServerWebExchange exchange) {
        log.error("Notification delivery failed: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, exchange);
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleNotificationNotFoundException(
            NotificationNotFoundException ex, ServerWebExchange exchange) {
        log.error("Notification not found: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, exchange);
    }

    // ==================== POST EXCEPTIONS ====================

    @ExceptionHandler(CommentDisabledException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCommentDisabledException(
            CommentDisabledException ex, ServerWebExchange exchange) {
        log.warn("Comment disabled: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, exchange);
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCommentNotFoundException(
            CommentNotFoundException ex, ServerWebExchange exchange) {
        log.error("Comment not found: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, exchange);
    }

    @ExceptionHandler(ContentViolationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleContentViolationException(
            ContentViolationException ex, ServerWebExchange exchange) {
        log.error("Content not found: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, exchange);
    }

    @ExceptionHandler(MediaSizeLimitExceededException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleMediaSizeLimitExceededException(
            MediaSizeLimitExceededException ex, ServerWebExchange exchange) {
        log.warn("Media size limit exceeded: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.CONTENT_TOO_LARGE, exchange);
    }

    @ExceptionHandler(MediaUploadFailedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleMediaUploadFailedException(
            MediaUploadFailedException ex, ServerWebExchange exchange) {
        log.error("Media upload failed: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, exchange);
    }

    @ExceptionHandler(PostDeletedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handlePostDeletedException(
            PostDeletedException ex, ServerWebExchange exchange) {
        log.warn("Post deleted: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.GONE, exchange);
    }

    @ExceptionHandler(PostNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handlePostNotFoundException(
            PostNotFoundException ex, ServerWebExchange exchange) {
        log.error("Post not found: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, exchange);
    }

    // ==================== SEARCH SERVICE EXCEPTIONS ====================

    @ExceptionHandler(SearchIndexUnavailableException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleSearchIndexUnavailableException(
            SearchIndexUnavailableException ex, ServerWebExchange exchange) {
        log.error("Search index unavailable: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, exchange);
    }

    @ExceptionHandler(SearchQueryInvalidException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleSearchQueryInvalidException(
            SearchQueryInvalidException ex, ServerWebExchange exchange) {
        log.error("Invalid search query: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, exchange);
    }

    @ExceptionHandler(SearchResultLimitExceededException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleSearchResultLimitExceededException(
            SearchResultLimitExceededException ex, ServerWebExchange exchange) {
        log.warn("Search result limit exceeded: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, exchange);
    }

    @ExceptionHandler(SearchTimeoutException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleSearchTimeoutException(
            SearchTimeoutException ex, ServerWebExchange exchange) {
        log.error("Search timeout: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.REQUEST_TIMEOUT, exchange);
    }

    // ==================== USER EXCEPTIONS ====================

    @ExceptionHandler(PrivacyRestrictionException.class)
    public Mono<ResponseEntity<ErrorResponse>> handlePrivacyRestrictionException(
            PrivacyRestrictionException ex, ServerWebExchange exchange) {
        log.warn("Privacy restriction: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, exchange);
    }

    @ExceptionHandler(ProfileUpdateNotAllowedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleProfileUpdateNotAllowedException(
            ProfileUpdateNotAllowedException ex, ServerWebExchange exchange) {
        log.warn("Profile update not allowed: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, exchange);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUserAlreadyExistsException(
            UserAlreadyExistsException ex, ServerWebExchange exchange) {
        log.error("User already exists: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.CONFLICT, exchange);
    }

    @ExceptionHandler(UserBlockedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUserBlockedException(
            UserBlockedException ex, ServerWebExchange exchange) {
        log.warn("User blocked: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, exchange);
    }

    @ExceptionHandler(UserDeactivatedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUserDeactivatedException(
            UserDeactivatedException ex, ServerWebExchange exchange) {
        log.warn("User deactivated: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, exchange);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUserNotFoundException(
            UserNotFoundException ex, ServerWebExchange exchange) {
        log.error("User not found: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, exchange);
    }

    // ==================== ADMIN SERVICE EXCEPTIONS ====================

    @ExceptionHandler(AdminPermissionDeniedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleAdminPermissionDeniedException(
            AdminPermissionDeniedException ex, ServerWebExchange exchange) {
        log.warn("Admin permission denied: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, exchange);
    }

    @ExceptionHandler(ModerationActionInvalidException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleModerationActionInvalidException(
            ModerationActionInvalidException ex, ServerWebExchange exchange) {
        log.error("Invalid moderation action: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, exchange);
    }

    @ExceptionHandler(SystemConfigUpdateFailedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleSystemConfigUpdateFailedException(
            SystemConfigUpdateFailedException ex, ServerWebExchange exchange) {
        log.error("System config update failed: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, exchange);
    }

    // ==================== ANALYTICS INGEST EXCEPTIONS ====================

    @ExceptionHandler(DuplicateEventException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDuplicateEventException(
            DuplicateEventException ex, ServerWebExchange exchange) {
        log.warn("Duplicate event: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.CONFLICT, exchange);
    }

    @ExceptionHandler(EventIngestFailedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleEventIngestFailedException(
            EventIngestFailedException ex, ServerWebExchange exchange) {
        log.error("Event ingest failed: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, exchange);
    }

    @ExceptionHandler(EventSchemaMismatchException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleEventSchemaMismatchException(
            EventSchemaMismatchException ex, ServerWebExchange exchange) {
        log.error("Event schema mismatch: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, exchange);
    }

    @ExceptionHandler(EventValidationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleEventValidationException(
            EventValidationException ex, ServerWebExchange exchange) {
        log.error("Event validation failed: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, exchange);
    }

    // ==================== CS SERVICE EXCEPTIONS ====================

    @ExceptionHandler(AppealNotAllowedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleAppealNotAllowedException(
            AppealNotAllowedException ex, ServerWebExchange exchange) {
        log.warn("Appeal not allowed: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, exchange);
    }

    @ExceptionHandler(ReportInvalidException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleReportInvalidException(
            ReportInvalidException ex, ServerWebExchange exchange) {
        log.error("Invalid report: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, exchange);
    }

    @ExceptionHandler(TicketAlreadyClosedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleTicketAlreadyClosedException(
            TicketAlreadyClosedException ex, ServerWebExchange exchange) {
        log.warn("Ticket already closed: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.CONFLICT, exchange);
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleTicketNotFoundException(
            TicketNotFoundException ex, ServerWebExchange exchange) {
        log.error("Ticket not found: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, exchange);
    }

    // ==================== FEED EXCEPTIONS ====================

    @ExceptionHandler(FeedGenerationFailedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleFeedGenerationFailedException(
            FeedGenerationFailedException ex, ServerWebExchange exchange) {
        log.error("Feed generation failed: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, exchange);
    }

    @ExceptionHandler(FeedNotAvailableException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleFeedNotAvailableException(
            FeedNotAvailableException ex, ServerWebExchange exchange) {
        log.error("Feed not available: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, exchange);
    }

    // ==================== VALIDATION EXCEPTION ====================

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
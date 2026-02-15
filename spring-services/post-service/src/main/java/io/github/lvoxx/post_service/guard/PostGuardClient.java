package io.github.lvoxx.post_service.guard;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.lvoxx.common_core.exception.model.post_exception.ContentViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Client for post-guard-service to validate content with AI.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostGuardClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${app.post-guard-service.url}")
    private String postGuardServiceUrl;

    @Value("${app.post-guard-service.timeout:10000}")
    private long timeout;

    /**
     * Validate post content using AI model.
     */
    public Mono<ValidationResult> validateContent(String content) {
        log.debug("Validating content with post-guard-service");

        WebClient webClient = webClientBuilder.baseUrl(postGuardServiceUrl).build();

        return webClient.post()
                .uri("/api/v1/validate")
                .bodyValue(new ContentRequest(content))
                .retrieve()
                .bodyToMono(ValidationResult.class)
                .timeout(Duration.ofMillis(timeout))
                .doOnSuccess(result -> {
                    if (!result.isValid()) {
                        log.warn("Content validation failed: {}", result.getReason());
                    }
                })
                .onErrorMap(e -> {
                    log.error("Error calling post-guard-service: {}", e.getMessage());
                    // If validation service is down, allow content for now
                    // In production, you might want to fail-close instead
                    return null;
                })
                .defaultIfEmpty(ValidationResult.valid());
    }

    /**
     * Validate and throw exception if content violates guidelines.
     */
    public Mono<Void> validateOrThrow(String content) {
        return validateContent(content)
                .flatMap(result -> {
                    if (!result.isValid()) {
                        return Mono.error(new ContentViolationException(result.getReason()));
                    }
                    return Mono.empty();
                });
    }

    /**
     * Content request DTO.
     */
    public record ContentRequest(String content) {
    }

    /**
     * Validation result DTO.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ValidationResult {
        private boolean valid;
        private String reason;

        public static ValidationResult valid() {
            return ValidationResult.builder()
                    .valid(true)
                    .build();
        }

        public static ValidationResult invalid(String reason) {
            return ValidationResult.builder()
                    .valid(false)
                    .reason(reason)
                    .build();
        }
    }
}
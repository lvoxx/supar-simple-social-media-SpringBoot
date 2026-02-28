package io.github.lvoxx.kafka_starter.envelope;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.f4b6a3.ulid.UlidCreator;

/**
 * Standard envelope for all Kafka events produced by X Social Platform
 * services.
 *
 * <p>All producers MUST wrap their domain payload in this envelope before
 * publishing to Kafka.
 * This ensures a consistent schema across all topics and enables uniform
 * processing
 * by consumers, DLT handlers, and the AI dashboard.</p>
 *
 * <h3>Schema:</h3>
 * <pre>{@code
 * {
 * "eventId": "01HXZ...", // ULID — unique event ID
 * "eventType": "post.created", // dot-notation domain event type
 * "version": "1", // schema version (increment on breaking change)
 * "timestamp": "2026-01-01T...", // UTC ISO-8601
 * "producerService":"post-service", // spring.application.name of the producer
 * "correlationId": "...", // trace/correlation ID for request tracking
 * "payload": { ... } // domain-specific event data
 * }
 * }</pre>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * var envelope = KafkaEventEnvelope.of("post.created", "1", serviceName,
 * correlationId, payload);
 * kafkaTemplate.send("post.created", postId, envelope).then();
 * }</pre>
 *
 * @param <T> type of the domain-specific payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record KafkaEventEnvelope<T>(

        /** Unique event identifier — ULID (time-sortable, UUID-compatible). */
        String eventId,

        /**
         * Dot-notation event type, e.g. {@code "post.created"},
         * {@code "user.followed"}.
         */
        String eventType,

        /**
         * Schema version. Start at {@code "1"}.
         * Breaking payload changes require a new event type (e.g.,
         * {@code "post.created.v2"}),
         * not a version bump.
         */
        String version,

        /** UTC timestamp of when the event was created by the producer. */
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp,

        /** {@code spring.application.name} of the service that produced this event. */
        String producerService,

        /** Correlation / trace ID forwarded from the originating HTTP request. */
        String correlationId,

        /** Domain-specific event payload. */
        T payload

) {
    /**
     * Factory method — generates a new ULID event ID and sets the timestamp to now.
     *
     * @param eventType       dot-notation event type
     * @param version         schema version string
     * @param producerService name of the producing service
     * @param correlationId   request trace ID (may be null for background events)
     * @param payload         domain-specific payload
     * @param <T>             payload type
     * @return fully populated event envelope
     */
    public static <T> KafkaEventEnvelope<T> of(
            String eventType,
            String version,
            String producerService,
            String correlationId,
            T payload) {
        return new KafkaEventEnvelope<>(
                UlidCreator.getMonotonicUlid().toString(),
                eventType,
                version,
                Instant.now(),
                producerService,
                correlationId,
                payload);
    }

    /**
     * Convenience factory without correlationId (for background/system events).
     */
    public static <T> KafkaEventEnvelope<T> of(
            String eventType,
            String version,
            String producerService,
            T payload) {
        return of(eventType, version, producerService, null, payload);
    }
}

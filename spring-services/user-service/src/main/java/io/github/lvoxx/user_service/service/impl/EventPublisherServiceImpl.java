package io.github.lvoxx.user_service.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import io.github.lvoxx.user_service.event.ElasticsearchSyncEvent;
import io.github.lvoxx.user_service.event.UserEvent;
import io.github.lvoxx.user_service.service.EventPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Spring Kafka-based event publisher service.
 * 
 * Uses {@link KafkaTemplate} with enhanced reliability features:
 * - Idempotent producer (exactly-once delivery)
 * - Acks=all (durability guarantee)
 * - Automatic retries with Integer.MAX_VALUE
 * - Observation/monitoring enabled
 * 
 * Note: KafkaTemplate operations are blocking, so we wrap them in
 * {@code Mono.fromFuture} and subscribe on {@code Schedulers.boundedElastic()}
 * to avoid blocking the reactive event loop.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisherServiceImpl implements EventPublisherService {

        private final KafkaTemplate<String, Object> kafkaTemplate;

        @Value("${app.kafka.topics.user-events}")
        private String userEventsTopic;

        @Value("${app.kafka.topics.elasticsearch-sync}")
        private String elasticsearchSyncTopic;

        @Override
        public Mono<Void> publishUserEvent(UserEvent event) {
                log.debug("Publishing user event: {} for user: {}", event.getEventType(), event.getUserId());

                return Mono.fromFuture(() -> kafkaTemplate.send(
                                userEventsTopic,
                                String.valueOf(event.getUserId()),
                                event))
                                .subscribeOn(Schedulers.boundedElastic())
                                .doOnSuccess(result -> {
                                        SendResult<String, Object> sendResult = result;
                                        var metadata = sendResult.getRecordMetadata();
                                        log.info(
                                                        "Event published successfully: {} to topic: {} partition: {} offset: {}",
                                                        event.getEventType(),
                                                        metadata.topic(),
                                                        metadata.partition(),
                                                        metadata.offset());
                                })
                                .doOnError(e -> log.error(
                                                "Error publishing event {}: {}",
                                                event.getEventType(),
                                                e.getMessage()))
                                .then()
                                .onErrorResume(e -> {
                                        // Don't fail the main operation if event publishing fails
                                        log.error("Failed to publish user event, continuing: {}", e.getMessage());
                                        return Mono.empty();
                                });
        }

        @Override
        public Mono<Void> publishElasticsearchSyncEvent(ElasticsearchSyncEvent event) {
                log.debug("Publishing Elasticsearch sync event: {} for document: {}",
                                event.getOperation(), event.getDocumentId());

                return Mono.fromFuture(() -> kafkaTemplate.send(
                                elasticsearchSyncTopic,
                                event.getDocumentId(),
                                event))
                                .subscribeOn(Schedulers.boundedElastic())
                                .doOnSuccess(result -> {
                                        SendResult<String, Object> sendResult = result;
                                        var metadata = sendResult.getRecordMetadata();
                                        log.info(
                                                        "Elasticsearch sync event published: {} for document: {} to partition: {} offset: {}",
                                                        event.getOperation(),
                                                        event.getDocumentId(),
                                                        metadata.partition(),
                                                        metadata.offset());
                                })
                                .doOnError(e -> log.error(
                                                "Error publishing Elasticsearch sync event: {}",
                                                e.getMessage()))
                                .then()
                                .onErrorResume(e -> {
                                        // Don't fail the main operation if event publishing fails
                                        log.error("Failed to publish Elasticsearch sync event, continuing: {}",
                                                        e.getMessage());
                                        return Mono.empty();
                                });
        }
}
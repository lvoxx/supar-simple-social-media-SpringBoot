package io.github.lvoxx.user_service.service.impl;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.lvoxx.user_service.event.UserEvent;
import io.github.lvoxx.user_service.service.EventPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Kafka-based event publisher service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisherServiceImpl implements EventPublisherService {

    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.user-events}")
    private String userEventsTopic;

    @Value("${app.kafka.topics.elasticsearch-sync}")
    private String elasticsearchSyncTopic;

    @Override
    public Mono<Void> publishUserEvent(UserEvent event) {
        log.debug("Publishing user event: {} for user: {}", event.getEventType(), event.getUserId());

        return Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
                .flatMap(json -> {
                    ProducerRecord<String, String> record = new ProducerRecord<>(
                            userEventsTopic,
                            String.valueOf(event.getUserId()),
                            json);

                    return kafkaSender.send(Mono.just(SenderRecord.create(record, event.getEventId())))
                            .next()
                            .doOnSuccess(result -> log.info(
                                    "Event published successfully: {} to topic: {} partition: {} offset: {}",
                                    event.getEventType(),
                                    userEventsTopic,
                                    result.recordMetadata().partition(),
                                    result.recordMetadata().offset()))
                            .doOnError(e -> log.error(
                                    "Error publishing event {}: {}",
                                    event.getEventType(),
                                    e.getMessage()))
                            .then();
                })
                .onErrorResume(e -> {
                    log.error("Failed to serialize or send event: {}", e.getMessage());
                    return Mono.empty(); // Don't fail the main operation
                });
    }

    @Override
    public Mono<Void> publishElasticsearchSyncEvent(ElasticsearchSyncEvent event) {
        log.debug("Publishing Elasticsearch sync event: {} for document: {}",
                event.getOperation(), event.getDocumentId());

        return Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
                .flatMap(json -> {
                    ProducerRecord<String, String> record = new ProducerRecord<>(
                            elasticsearchSyncTopic,
                            event.getDocumentId(),
                            json);

                    return kafkaSender.send(Mono.just(SenderRecord.create(record, event.getEventId())))
                            .next()
                            .doOnSuccess(result -> log.info(
                                    "Elasticsearch sync event published: {} for document: {} to partition: {} offset: {}",
                                    event.getOperation(),
                                    event.getDocumentId(),
                                    result.recordMetadata().partition(),
                                    result.recordMetadata().offset()))
                            .doOnError(e -> log.error(
                                    "Error publishing Elasticsearch sync event: {}",
                                    e.getMessage()))
                            .then();
                })
                .onErrorResume(e -> {
                    log.error("Failed to serialize or send Elasticsearch sync event: {}", e.getMessage());
                    return Mono.empty();
                });
    }
}
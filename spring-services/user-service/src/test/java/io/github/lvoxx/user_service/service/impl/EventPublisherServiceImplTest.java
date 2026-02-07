
package io.github.lvoxx.user_service.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.lvoxx.user_service.event.ElasticsearchSyncEvent;
import io.github.lvoxx.user_service.event.UserCreatedEvent;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link EventPublisherServiceImpl} using Spring Kafka's
 * KafkaTemplate.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventPublisherServiceImpl unit tests")
class EventPublisherServiceImplTest {

        @Mock
        private KafkaTemplate<String, Object> kafkaTemplate;

        @InjectMocks
        private EventPublisherServiceImpl service;

        @BeforeEach
        void injectTopicNames() {
                ReflectionTestUtils.setField(service, "userEventsTopic", "user.events.test");
                ReflectionTestUtils.setField(service, "elasticsearchSyncTopic", "elasticsearch.sync.test");
        }

        // ---------------------------------------------------------------------------
        private SendResult<String, Object> stubSendResult(String topic) {
                RecordMetadata metadata = new RecordMetadata(
                                new TopicPartition(topic, 0),
                                0L, // offset
                                0, // serializedKeySize
                                0L, // timestamp
                                0, // serializedValueSize
                                0 // serializedHeadersSize
                );

                ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(topic, "key", "value");

                return new SendResult<>(producerRecord, metadata);
        }

        // =========================================================================
        @Nested
        @DisplayName("publishUserEvent")
        class PublishUserEvent {

                @Test
                @DisplayName("success: sends event to Kafka topic")
                void success() {
                        UserCreatedEvent event = UserCreatedEvent.builder()
                                        .eventId("evt-1")
                                        .eventType("USER_CREATED")
                                        .userId(1L)
                                        .username("john")
                                        .email("john@x.com")
                                        .displayName("John")
                                        .keycloakUserId("kc-1")
                                        .timestamp(LocalDateTime.now())
                                        .source("user-service")
                                        .build();

                        SendResult<String, Object> sendResult = stubSendResult("user.events.test");
                        CompletableFuture<SendResult<String, Object>> future = CompletableFuture
                                        .completedFuture(sendResult);

                        when(kafkaTemplate.send(eq("user.events.test"), anyString(), any()))
                                        .thenReturn(future);

                        StepVerifier.create(service.publishUserEvent(event))
                                        .verifyComplete();

                        verify(kafkaTemplate).send(eq("user.events.test"), eq("1"), eq(event));
                }

                @Test
                @DisplayName("error: completes empty (does not propagate)")
                void error_completesEmpty() {
                        UserCreatedEvent event = UserCreatedEvent.builder()
                                        .eventId("evt-err")
                                        .eventType("USER_CREATED")
                                        .userId(1L)
                                        .timestamp(LocalDateTime.now())
                                        .source("user-service")
                                        .build();

                        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
                        future.completeExceptionally(new RuntimeException("Kafka error"));

                        when(kafkaTemplate.send(eq("user.events.test"), anyString(), any()))
                                        .thenReturn(future);

                        StepVerifier.create(service.publishUserEvent(event))
                                        .verifyComplete();
                }
        }

        // =========================================================================
        @Nested
        @DisplayName("publishElasticsearchSyncEvent")
        class PublishEsSyncEvent {

                @Test
                @DisplayName("success: sends INDEX event")
                void success() {
                        ElasticsearchSyncEvent event = ElasticsearchSyncEvent.builder()
                                        .eventId("es-1")
                                        .indexName("users")
                                        .documentId("1")
                                        .operation("INDEX")
                                        .document(Map.of("username", "john"))
                                        .timestamp(LocalDateTime.now())
                                        .source("user-service")
                                        .build();

                        SendResult<String, Object> sendResult = stubSendResult("elasticsearch.sync.test");
                        CompletableFuture<SendResult<String, Object>> future = CompletableFuture
                                        .completedFuture(sendResult);

                        when(kafkaTemplate.send(eq("elasticsearch.sync.test"), anyString(), any()))
                                        .thenReturn(future);

                        StepVerifier.create(service.publishElasticsearchSyncEvent(event))
                                        .verifyComplete();

                        verify(kafkaTemplate).send(eq("elasticsearch.sync.test"), eq("1"), eq(event));
                }

                @Test
                @DisplayName("error: completes empty (does not propagate)")
                void error_completesEmpty() {
                        ElasticsearchSyncEvent event = ElasticsearchSyncEvent.builder()
                                        .eventId("es-err")
                                        .indexName("users")
                                        .documentId("1")
                                        .operation("DELETE")
                                        .timestamp(LocalDateTime.now())
                                        .source("user-service")
                                        .build();

                        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
                        future.completeExceptionally(new RuntimeException("Kafka down"));

                        when(kafkaTemplate.send(eq("elasticsearch.sync.test"), anyString(), any()))
                                        .thenReturn(future);

                        StepVerifier.create(service.publishElasticsearchSyncEvent(event))
                                        .verifyComplete();
                }
        }
}
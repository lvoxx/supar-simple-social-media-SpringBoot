package io.github.lvoxx.kafka_starter.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import io.github.lvoxx.kafka_starter.properties.KafkaProperties;
import lombok.extern.slf4j.Slf4j;
import reactor.kafka.sender.SenderOptions;

/**
 * Auto-configuration for Kafka integration (Spring Boot 4.0.2 / Spring Kafka
 * 4.x).
 *
 * <h3>What this starter provides:</h3>
 * <ul>
 * <li>{@link ReactiveKafkaProducerTemplate} -- reactive producer with Jackson 3
 * JSON serialization,
 * idempotence, and {@code acks=all}</li>
 * <li>{@link KafkaProperties} -- typed config for retry and DLT settings</li>
 * <li>{@link io.github.lvoxx.starter.kafka.envelope.KafkaEventEnvelope} --
 * standard event wrapper</li>
 * </ul>
 *
 * <h3>Jackson 3 (Spring Boot 4):</h3>
 * Spring Kafka 4.x ({@code spring-boot-kafka-starter}) ships updated
 * {@code JsonSerializer} / {@code JsonDeserializer} that use Jackson 3's
 * {@code tools.jackson.databind.JsonMapper} internally. No manual Jackson
 * version
 * configuration is needed -- just add the starter and Jackson 3 is used
 * automatically.
 *
 * <h3>Consumer configuration:</h3>
 * Services create their own {@code ReactiveKafkaConsumerTemplate} beans using
 * {@link #consumerProps(String, String)} as a base. This avoids coupling
 * consumers to the starter.
 *
 * <h3>DLT and retry:</h3>
 * Use {@code @RetryableTopic} with values from {@code sssm.kafka.retry.*} on
 * consumer methods.
 * Each topic automatically gets a {@code <topic>.DLT} dead-letter topic.
 */
@Slf4j
@Configuration
@ConditionalOnClass(ReactiveKafkaProducerTemplate.class)
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaStarterAutoConfiguration {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.application.name:unknown-service}")
    private String applicationName;

    /**
     * Reactive Kafka producer template shared across all producers in a service.
     *
     * <p>
     * Producer guarantees:
     * </p>
     * <ul>
     * <li>{@code acks=all} -- waits for all in-sync replicas to acknowledge</li>
     * <li>{@code enable.idempotence=true} -- exactly-once producer semantics</li>
     * <li>{@code retries=3} with
     * {@code max.in.flight.requests.per.connection=1}</li>
     * <li>Jackson 3 JSON value serialization via Spring Kafka's updated
     * {@link JsonSerializer}</li>
     * </ul>
     */
    @Bean
    @ConditionalOnMissingBean(ReactiveKafkaProducerTemplate.class)
    public ReactiveKafkaProducerTemplate<String, Object> reactiveKafkaProducerTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        // Jackson 3: type headers not needed (all events use KafkaEventEnvelope
        // wrapper)
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        log.info("[kafka-starter] Registering ReactiveKafkaProducerTemplate for service='{}'", applicationName);
        return new ReactiveKafkaProducerTemplate<>(SenderOptions.create(props));
    }

    /**
     * Returns a base consumer properties map for use when creating
     * {@code ReactiveKafkaConsumerTemplate} beans in services.
     *
     * <h3>Usage in a service:</h3>
     *
     * <pre>{@code
     * @Bean
     * public ReactiveKafkaConsumerTemplate<String, KafkaEventEnvelope<PostCreatedPayload>> postConsumer(
     *         KafkaStarterAutoConfiguration kafkaConfig) {
     *     var opts = ReceiverOptions
     *             .<String, KafkaEventEnvelope<PostCreatedPayload>>create(
     *                     kafkaConfig.consumerProps(PostCreatedPayload.class.getName()))
     *             .subscription(List.of("post.created"));
     *     return new ReactiveKafkaConsumerTemplate<>(opts);
     * }
     * }</pre>
     *
     * @param targetValueType fully qualified class name of the expected payload
     *                        type
     * @return consumer properties map with Jackson 3 deserializer configuration
     */
    public Map<String, Object> consumerProps(String targetValueType) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, applicationName);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "io.github.lvoxx.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, targetValueType);
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put("isolation.level", "read_committed");
        return props;
    }
}

package io.github.lvoxx.kafka_starter.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaBootstrapConfiguration;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import io.github.lvoxx.kafka_starter.properties.KafkaProperties;
import lombok.extern.slf4j.Slf4j;
import reactor.kafka.sender.SenderOptions;

/**
 * Auto-configuration for Kafka integration.
 *
 * <h3>What this starter provides:</h3>
 * <ul>
 * <li>{@link ReactiveKafkaProducerTemplate} — reactive Kafka producer with JSON
 * serialization,
 * idempotence, and {@code acks=all}</li>
 * <li>{@link KafkaProperties} bound to {@code xsocial.kafka.*} for retry and
 * DLT config</li>
 * <li>{@link io.github.lvoxx.starter.kafka.envelope.KafkaEventEnvelope} —
 * standard event wrapper
 * (no Spring bean, just a shared POJO)</li>
 * </ul>
 *
 * <h3>Consumer setup:</h3>
 * Services create their own {@code ReactiveKafkaConsumerTemplate} beans with
 * the
 * {@link #consumerProps(String, String)} helper to avoid boilerplate.
 *
 * <h3>DLT and retry:</h3>
 * Consumers use Spring Kafka's {@code @RetryableTopic} with the retry
 * parameters
 * from {@code xsocial.kafka.retry.*}.
 */
@Slf4j
@AutoConfiguration(after = KafkaBootstrapConfiguration.class)
@ConditionalOnClass({ ReactiveKafkaProducerTemplate.class })
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
     * Configuration:
     * </p>
     * <ul>
     * <li>{@code acks=all} — strongest durability guarantee</li>
     * <li>{@code enable.idempotence=true} — exactly-once producer semantics</li>
     * <li>{@code retries=3} with
     * {@code max.in.flight.requests.per.connection=1}</li>
     * <li>JSON value serialization via {@link JsonSerializer}</li>
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
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        SenderOptions<String, Object> senderOptions = SenderOptions.create(props);
        log.info("[starter-kafka] Registering ReactiveKafkaProducerTemplate for service='{}'", applicationName);
        return new ReactiveKafkaProducerTemplate<>(senderOptions);
    }

    /**
     * Utility method for building consumer properties map.
     * Services call this when creating their own
     * {@code ReactiveKafkaConsumerTemplate} beans.
     *
     * <p>
     * Usage in a service:
     * </p>
     * 
     * <pre>{@code
     * &#64;Bean
     * public ReactiveKafkaConsumerTemplate<String, KafkaEventEnvelope<PostCreatedPayload>> postConsumer(
     *         KafkaStarterAutoConfiguration kafkaConfig) {
     *     ReceiverOptions<String, ...> opts = ReceiverOptions.<String, KafkaEventEnvelope<PostCreatedPayload>>create(
     *             kafkaConfig.consumerProps("post.created", PostCreatedPayload.class.getName()))
     *         .subscription(List.of("post.created"));
     *     return new ReactiveKafkaConsumerTemplate<>(opts);
     * }
     * }</pre>
     *
     * @param topic     topic name (used to name the consumer group)
     * @param valueType fully qualified class name of the expected payload type
     */
    public Map<String, Object> consumerProps(String topic, String valueType) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, applicationName);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "io.github.lvoxx.*,com.xsocial.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, valueType);
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put("isolation.level", "read_committed");
        return props;
    }
}

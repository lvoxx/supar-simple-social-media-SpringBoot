package io.github.lvoxx.kafka_producer_starter.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.transaction.KafkaTransactionManager;

/**
 * Kafka Producer Configuration.
 * 
 * This configuration class sets up Kafka producer infrastructure with enhanced
 * reliability and idempotency guarantees. It configures the producer factory,
 * Kafka template, and transaction management for reliable message publishing.
 * 
 * Key features:
 * - Idempotent producer to prevent duplicate messages
 * - Acknowledgment from all in-sync replicas for durability
 * - Automatic retry mechanism with maximum retries
 * - Transaction support for exactly-once semantics
 * - Observation/monitoring support
 * 
 * @see ProducerFactory
 * @see KafkaTemplate
 * @see KafkaTransactionManager
 */
@Configuration
@EnableKafka
public class KafkaProducerConfig {

    /**
     * Creates and configures the Kafka producer factory with enhanced reliability
     * settings.
     * 
     * This factory is configured with:
     * - Acknowledgment from all in-sync replicas (acks=all) for maximum durability
     * - Idempotent producer enabled to prevent duplicate message delivery
     * - Retry count set to Integer.MAX_VALUE for automatic recovery from transient
     * failures
     * - Max in-flight requests set to 5 for optimized throughput without
     * sacrificing ordering
     * 
     * @param properties Spring Kafka properties for baseline configuration
     * @return a configured ProducerFactory instance for String keys and Object
     *         values
     */
    @Bean
    ProducerFactory<String, Object> producerFactory(KafkaProperties properties) {
        Map<String, Object> config = new HashMap<>(properties.buildProducerProperties());

        // Hard override to prevent accidental misconfig
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * Creates a Kafka template for sending messages to Kafka topics.
     * 
     * The template is configured with observation/monitoring enabled to allow
     * integration with observability frameworks for tracing and metrics collection.
     * This template should be used as the primary bean for publishing messages
     * to Kafka topics throughout the application.
     * 
     * @param pf the ProducerFactory configured in this configuration
     * @return a KafkaTemplate instance with monitoring support enabled
     */
    @Bean
    KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> pf) {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(pf);
        template.setObservationEnabled(true);
        return template;
    }

    /**
     * Creates a Kafka transaction manager for managing distributed transactions.
     * 
     * This transaction manager enables exactly-once semantics for message
     * publishing,
     * ensuring that messages are delivered reliably within a transactional context.
     * It should be used in conjunction with Spring's {@code @Transactional}
     * annotation
     * to coordinate Kafka producer operations with other transactional resources.
     * 
     * @param pf the ProducerFactory configured in this configuration
     * @return a KafkaTransactionManager instance for handling Kafka transactions
     */
    @Bean
    KafkaTransactionManager<String, Object> kafkaTransactionManager(
            ProducerFactory<String, Object> pf) {
        return new KafkaTransactionManager<>(pf);
    }
}

package io.github.lvoxx.kafka_consumer_starter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;

/**
 * Kafka consumer configuration class for the Kafka consumer starter module.
 * 
 * <p>This configuration class sets up the Kafka listener container factory with the following features:
 * <ul>
 *   <li>Batch message listening enabled</li>
 *   <li>Manual acknowledgment mode for fine-grained control over message processing</li>
 *   <li>Custom error handling with the provided {@link DefaultErrorHandler}</li>
 *   <li>Observation enabled for metrics and monitoring</li>
 *   <li>Optimized poll timeout to avoid rebalance storms</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    /**
     * Creates and configures a {@link ConcurrentKafkaListenerContainerFactory} bean for handling
     * Kafka messages in batch mode with manual acknowledgment.
     *
     * <p>Key configurations:
     * <ul>
     *   <li>Batch listening is enabled for processing multiple messages at once</li>
     *   <li>Acknowledgment mode is set to MANUAL for explicit control over message commits</li>
     *   <li>Observation is enabled to track message processing metrics</li>
     *   <li>Poll timeout is set to 1500ms to prevent rebalance storms during high-load scenarios</li>
     * </ul>
     * </p>
     *
     * @param cf the {@link ConsumerFactory} for creating Kafka consumer instances
     * @param errorHandler the {@link DefaultErrorHandler} for handling consumer errors
     * @return a configured {@link ConcurrentKafkaListenerContainerFactory} instance
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> cf,
            DefaultErrorHandler errorHandler
    ) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(cf);
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(errorHandler);

        // tránh rebalance storm
        factory.getContainerProperties().setObservationEnabled(true);
        factory.getContainerProperties().setPollTimeout(1500);

        return factory;
    }
}
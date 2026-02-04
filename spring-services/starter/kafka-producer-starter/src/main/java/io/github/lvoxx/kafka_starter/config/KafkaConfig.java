package io.github.lvoxx.kafka_starter.config;

import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reactor-Kafka {@link KafkaSender} configuration.
 *
 * {@code reactor-kafka} does not ship a Spring Boot auto-configuration class,
 * so a single bean definition is still needed here. However, all producer
 * properties are read from the standard {@code spring.kafka.producer.*}
 * namespace
 * via the auto-configured {@link KafkaProperties} bean – no values are
 * hard-coded.
 */
@Configuration
public class KafkaConfig {

    /**
     * Creates a {@link KafkaSender} whose producer properties are sourced entirely
     * from {@code spring.kafka.*} configuration (bootstrap-servers + producer.*).
     */
    @Bean
    public KafkaSender<String, String> kafkaSender(KafkaProperties kafkaProperties) {
        // KafkaProperties already merges bootstrap-servers into the producer map
        var producerProps = kafkaProperties.buildProducerProperties();

        SenderOptions<String, String> senderOptions = SenderOptions.create(producerProps);

        return KafkaSender.create(senderOptions);
    }
}
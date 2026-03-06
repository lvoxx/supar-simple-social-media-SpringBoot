package io.github.lvoxx.kafka_starter.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;

import io.github.lvoxx.kafka_starter.properties.KafkaStarterProperties;
import reactor.kafka.sender.SenderOptions;

@AutoConfiguration
@ConditionalOnClass(ReactiveKafkaProducerTemplate.class)
@EnableConfigurationProperties(KafkaStarterProperties.class)
public class KafkaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ReactiveKafkaProducerTemplate<String, Object> reactiveKafkaProducerTemplate(
            org.springframework.boot.autoconfigure.kafka.KafkaProperties kafkaProperties) {

        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties(null));
        // Enforce idempotent producer settings
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        SenderOptions<String, Object> senderOptions = SenderOptions.create(props);
        return new ReactiveKafkaProducerTemplate<>(senderOptions);
    }
}

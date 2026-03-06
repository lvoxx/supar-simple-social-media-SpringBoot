package io.github.lvoxx.kafka_starter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "sssm.kafka")
public class KafkaStarterProperties {

    private Retry retry = new Retry();
    private Dlt dlt = new Dlt();

    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private double backoffMultiplier = 2.0;
        private long initialIntervalMs = 200;
    }

    @Data
    public static class Dlt {
        private boolean enabled = true;
    }
}

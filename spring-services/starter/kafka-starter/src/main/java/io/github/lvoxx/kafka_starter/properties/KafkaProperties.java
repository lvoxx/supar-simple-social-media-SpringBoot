package io.github.lvoxx.kafka_starter.properties;

/**
 * XSocial-specific Kafka extension properties.
 * Standard Spring Kafka producer/consumer properties are configured via
 * {@code spring.kafka.*}.
 */
@ConfigurationProperties(prefix = "xsocial.kafka")
public class KafkaProperties {

    private final Retry retry = new Retry();
    private final Dlt dlt = new Dlt();

    public Retry getRetry() {
        return retry;
    }

    public Dlt getDlt() {
        return dlt;
    }

    public static class Retry {
        /** Maximum number of retry attempts before sending to DLT. */
        private int maxAttempts = 3;
        /** Initial backoff interval for the first retry. */
        private Duration initialInterval = Duration.ofMillis(200);
        /** Backoff multiplier for each subsequent retry. */
        private double backoffMultiplier = 2.0;
        /** Maximum backoff interval cap. */
        private Duration maxInterval = Duration.ofSeconds(10);

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getInitialInterval() {
            return initialInterval;
        }

        public void setInitialInterval(Duration initialInterval) {
            this.initialInterval = initialInterval;
        }

        public double getBackoffMultiplier() {
            return backoffMultiplier;
        }

        public void setBackoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
        }

        public Duration getMaxInterval() {
            return maxInterval;
        }

        public void setMaxInterval(Duration maxInterval) {
            this.maxInterval = maxInterval;
        }
    }

    public static class Dlt {
        /** Whether dead letter topic routing is enabled. */
        private boolean enabled = true;
        /** Suffix appended to the original topic name to form the DLT topic. */
        private String suffix = ".DLT";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSuffix() {
            return suffix;
        }

        public void setSuffix(String suffix) {
            this.suffix = suffix;
        }
    }
}

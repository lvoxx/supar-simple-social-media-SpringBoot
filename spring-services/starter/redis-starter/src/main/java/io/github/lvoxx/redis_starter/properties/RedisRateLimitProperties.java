package io.github.lvoxx.redis_starter.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * XSocial-specific extension properties for the Redis starter.
 * Standard Spring Boot Redis/Cache properties ({@code spring.data.redis.*},
 * {@code spring.cache.*}) are still used for core connection configuration.
 */
@ConfigurationProperties(prefix = "xsocial.rate-limit")
public class RedisRateLimitProperties {

    /** Whether rate limiting is active globally. */
    private boolean enabled = true;

    /**
     * Default bucket capacity (max tokens allowed at any time).
     * Each request consumes 1 token.
     */
    private long defaultCapacity = 100;

    /**
     * Number of tokens refilled per period.
     * Together with {@code defaultRefillPeriod}, defines the sustained throughput.
     */
    private long defaultRefillTokens = 100;

    /** Refill period for the default bucket. */
    private Duration defaultRefillPeriod = Duration.ofMinutes(1);

    /** Redis key prefix for rate-limit buckets. */
    private String keyPrefix = "xsocial:rate-limit:";

    // -------------------------------------------------------------------------
    // Getters & setters
    // -------------------------------------------------------------------------

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getDefaultCapacity() {
        return defaultCapacity;
    }

    public void setDefaultCapacity(long defaultCapacity) {
        this.defaultCapacity = defaultCapacity;
    }

    public long getDefaultRefillTokens() {
        return defaultRefillTokens;
    }

    public void setDefaultRefillTokens(long defaultRefillTokens) {
        this.defaultRefillTokens = defaultRefillTokens;
    }

    public Duration getDefaultRefillPeriod() {
        return defaultRefillPeriod;
    }

    public void setDefaultRefillPeriod(Duration defaultRefillPeriod) {
        this.defaultRefillPeriod = defaultRefillPeriod;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }
}

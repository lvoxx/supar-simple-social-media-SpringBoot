package io.github.lvoxx.redis_starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitConfig {

    @Data
    public static class MediaService {
        private RateLimitSettings view;
        private RateLimitSettings original;
        private RateLimitSettings upload;
    }

    @Data
    public static class RateLimitSettings {
        private long capacity;
        private long refillTokens;
        private long refillDuration;
    }
}

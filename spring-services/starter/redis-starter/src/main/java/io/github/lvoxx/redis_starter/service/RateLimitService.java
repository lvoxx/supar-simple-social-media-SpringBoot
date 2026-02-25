package io.github.lvoxx.redis_starter.service;

import java.time.Duration;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import io.github.lvoxx.redis_starter.config.RateLimitConfig;
import io.github.lvoxx.redis_starter.utils.RateLimitType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RateLimitConfig.MediaService rateLimitConfig;

    private static final String RATE_LIMIT_PREFIX = "rate-limit:";

    public Mono<Boolean> checkRateLimit(String key, RateLimitType.MediaService type) {
        RateLimitConfig.RateLimitSettings settings = getMediaServiceSettings(type);
        String redisKey = RATE_LIMIT_PREFIX + type.name().toLowerCase() + ":" + key;

        return redisTemplate.opsForValue().get(redisKey)
                .flatMap(currentValue -> {
                    long current = Long.parseLong(currentValue);
                    if (current >= settings.getCapacity()) {
                        log.warn("Rate limit exceeded for key: {} type: {}", key, type);
                        return Mono.just(false);
                    }
                    return redisTemplate.opsForValue().increment(redisKey)
                            .map(newValue -> true);
                })
                .switchIfEmpty(
                        redisTemplate.opsForValue()
                                .set(redisKey, "1", Duration.ofSeconds(settings.getRefillDuration()))
                                .thenReturn(true))
                .onErrorResume(e -> {
                    log.error("Error checking rate limit: {}", e.getMessage());
                    return Mono.just(true);
                });
    }

    private RateLimitConfig.RateLimitSettings getMediaServiceSettings(RateLimitType.MediaService type) {
        return switch (type) {
            case VIEW -> rateLimitConfig.getView();
            case ORIGINAL -> rateLimitConfig.getOriginal();
            case UPLOAD -> rateLimitConfig.getUpload();
        };
    }

}
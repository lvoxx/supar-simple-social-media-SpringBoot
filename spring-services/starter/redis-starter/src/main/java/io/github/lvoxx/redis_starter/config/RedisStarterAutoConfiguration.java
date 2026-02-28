package io.github.lvoxx.redis_starter.config;

import java.time.Duration;

import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager;
import io.github.lvoxx.redis_starter.properties.RedisRateLimitProperties;
import io.github.lvoxx.redis_starter.ratelimit.RateLimiterService;

/**
 * Auto-configuration for Redis integration.
 *
 * <h3>What this starter provides:</h3>
 * <ul>
 * <li>{@link ReactiveRedisTemplate} — reactive Redis operations with JSON
 * serialization</li>
 * <li>{@link RedissonClient} — managed by Redisson's own
 * auto-configuration</li>
 * <li>{@link RedisCacheManager} — Spring Cache abstraction backed by Redis</li>
 * <li>{@link RateLimiterService} — Bucket4j token bucket stored in Redis
 * (distributed)</li>
 * </ul>
 *
 * <h3>Caching usage:</h3>
 * 
 * <pre>{@code
 * &#64;Cacheable(value = "user:profile", key = "#userId")
 * public Mono<UserResponse> findById(String userId) { ... }
 * }</pre>
 *
 * <h3>Distributed lock usage:</h3>
 * 
 * <pre>{@code
 * RLock lock = redissonClient.getLock("lock:follow:" + userId);
 * Mono.fromCallable(() -> lock.tryLock(500, 5000, TimeUnit.MILLISECONDS))
 *         .flatMap(acquired -> acquired
 *                 ? performFollow().doFinally(s -> lock.unlock())
 *                 : Mono.error(new ConflictException("OPERATION_IN_PROGRESS")));
 * }</pre>
 */
@AutoConfiguration(after = { RedisStarterAutoConfiguration.class, RedisReactiveAutoConfiguration.class,
        CacheAutoConfiguration.class })
@ConditionalOnClass({ ReactiveRedisTemplate.class, RedissonClient.class })
@EnableConfigurationProperties(RedisRateLimitProperties.class)
@EnableCaching
public class RedisStarterAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RedisStarterAutoConfiguration.class);

    /**
     * Reactive Redis template with String keys and JSON-serialized values.
     *
     * <p>
     * Uses {@link GenericJackson2JsonRedisSerializer} for values so that arbitrary
     * objects can be stored and retrieved without explicit type mapping.
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(name = "reactiveRedisTemplate")
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

        RedisSerializationContext<String, Object> context = RedisSerializationContext
                .<String, Object>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();

        log.info("[starter-redis] Registering ReactiveRedisTemplate with JSON serialization");
        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    /**
     * Spring Cache manager backed by Redis.
     *
     * <p>
     * Default TTL is 5 minutes. Null values are NOT cached.
     * Override per cache: use {@code @Cacheable} with a custom {@code cacheManager}
     * bean
     * or configure {@code spring.cache.redis.time-to-live}.
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(RedisCacheManager.class)
    public RedisCacheManager cacheManager(
            ReactiveRedisConnectionFactory connectionFactory) {

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .prefixCacheNameWith("xsocial:");

        log.info("[starter-redis] Registering RedisCacheManager with default TTL=5m");
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .build();
    }

    /**
     * Bucket4j distributed rate limiter backed by Redisson.
     * Only registered if rate limiting is enabled (default: true).
     */
    @Bean
    @ConditionalOnMissingBean(RateLimiterService.class)
    @ConditionalOnProperty(prefix = "xsocial.rate-limit", name = "enabled", matchIfMissing = true)
    public RateLimiterService rateLimiterService(
            RedissonClient redissonClient,
            RedisRateLimitProperties properties) {

        ProxyManager<String> proxyManager = RedissonBasedProxyManager
                .builderFor(redissonClient)
                .build();

        log.info("[starter-redis] Registering RateLimiterService (Bucket4j + Redisson)");
        return new RateLimiterService(proxyManager, properties);
    }
}

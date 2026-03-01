package io.github.lvoxx.redis_starter.config;

import java.time.Duration;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.command.CommandAsyncExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager;
import io.github.lvoxx.redis_starter.properties.RedisRateLimitProperties;
import io.github.lvoxx.redis_starter.ratelimit.RateLimiterService;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * Auto-configuration for Redis integration (Spring Boot 4.0.2 / Spring Data
 * Redis 4.x).
 *
 * <h3>What this starter provides:</h3>
 * <ul>
 * <li>{@link ReactiveRedisTemplate} -- reactive Redis operations with Jackson 3
 * JSON serialization</li>
 * <li>{@link RedisCacheManager} -- Spring {@code @Cacheable} backed by Redis
 * (TTL 5 min default)</li>
 * <li>{@link RateLimiterService} -- Bucket4j distributed token bucket via
 * Redisson</li>
 * <li>{@link RedissonClient} -- managed by Redisson's own Spring Boot
 * auto-configuration</li>
 * </ul>
 *
 * <h3>Jackson 3 serialization:</h3>
 * Spring Boot 4 defaults to Jackson 3 ({@code tools.jackson.*}).
 * Spring Data Redis 4.x provides
 * {@link org.springframework.data.redis.serializer.GenericJackson3JsonRedisSerializer}
 * for Jackson 3-native JSON serialization. This replaces the Jackson 2-era
 * {@code GenericJackson2JsonRedisSerializer}.
 *
 * <h3>Caching usage:</h3>
 * 
 * <pre>
 * {@code
 * &#64;Cacheable(value = "user:profile", key = "#userId")
 * public Mono<UserResponse> findById(String userId) { ... }
 *
 * &#64;CacheEvict(value = "user:profile", key = "#userId")
 * public Mono<UserResponse> updateProfile(String userId, ...) { ... }
 * }
 * </pre>
 *
 * <h3>Distributed lock usage:</h3>
 * 
 * <pre>{@code
 * RLock lock = redissonClient.getLock("lock:follow:" + userId);
 * Mono.fromCallable(() -> lock.tryLock(500, 5000, TimeUnit.MILLISECONDS))
 *                 .flatMap(acquired -> acquired
 *                                 ? performFollow().doFinally(s -> lock.unlock())
 *                                 : Mono.error(new ConflictException("OPERATION_IN_PROGRESS")));
 * }</pre>
 */
@Slf4j
@AutoConfiguration(after = CacheAutoConfiguration.class)
@ConditionalOnClass({ ReactiveRedisTemplate.class, RedissonClient.class })
@EnableConfigurationProperties(RedisRateLimitProperties.class)
public class RedisStarterAutoConfiguration {

        /**
         * Reactive Redis template using Jackson 3 for value serialization.
         *
         * <p>
         * Spring Data Redis 4.x ships {@code GenericJackson3JsonRedisSerializer} which
         * uses
         * {@code tools.jackson.databind.JsonMapper} (Jackson 3). Keys are always plain
         * strings.
         * </p>
         */
        @Bean
        @ConditionalOnMissingBean(name = "reactiveRedisTemplate")
        public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
                        ReactiveRedisConnectionFactory connectionFactory,
                        ObjectMapper objectMapper) {

                var keySerializer = new StringRedisSerializer();

                var valueSerializer = new GenericJacksonJsonRedisSerializer(objectMapper);

                var context = RedisSerializationContext
                                .<String, Object>newSerializationContext(keySerializer)
                                .value(valueSerializer)
                                .hashKey(keySerializer)
                                .hashValue(valueSerializer)
                                .build();

                return new ReactiveRedisTemplate<>(connectionFactory, context);
        }

        /**
         * Spring Cache manager backed by Redis with a 5-minute default TTL.
         * Null values are not cached. All keys are prefixed with {@code sssm:}.
         */
        @Bean
        @ConditionalOnMissingBean(RedisCacheManager.class)
        public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(5))
                                .disableCachingNullValues()
                                .prefixCacheNameWith("sssm:");

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultConfig)
                                .build();
        }

        /**
         * Bucket4j distributed rate limiter using Redisson as the storage backend.
         * Conditionally disabled via {@code sssm.rate-limit.enabled=false}.
         */
        @Bean
        @ConditionalOnMissingBean(RateLimiterService.class)
        @ConditionalOnProperty(prefix = "sssm.rate-limit", name = "enabled", matchIfMissing = true)
        public RateLimiterService rateLimiterService(
                        RedissonClient redissonClient,
                        RedisRateLimitProperties properties) {

                CommandAsyncExecutor executor = ((Redisson) redissonClient).getCommandExecutor();

                ProxyManager<String> proxyManager = RedissonBasedProxyManager
                                .builderFor(executor)
                                .build();

                return new RateLimiterService(proxyManager, properties);
        }
}

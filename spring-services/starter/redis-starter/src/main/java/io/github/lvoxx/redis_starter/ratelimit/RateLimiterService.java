package io.github.lvoxx.redis_starter.ratelimit;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.lvoxx.redis_starter.properties.RedisRateLimitProperties;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Reactive rate limiter backed by Bucket4j distributed token buckets stored in
 * Redis.
 *
 * <p>
 * Each unique {@code (key, endpoint)} combination gets its own bucket.
 * Buckets are stored in Redis so limits are enforced across all pod replicas.
 * </p>
 *
 * <h3>Usage (in a WebFilter):</h3>
 * 
 * <pre>{@code
 * return rateLimiterService.tryConsume(userId, "/api/v1/posts")
 *         .flatMap(allowed -> allowed
 *                 ? chain.filter(exchange)
 *                 : buildRateLimitResponse(exchange));
 * }</pre>
 *
 * <h3>Per-endpoint customization:</h3>
 * Override bucket configuration by calling
 * {@link #tryConsume(String, String, long, long, Duration)} with custom
 * capacity and refill params.
 */
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final ProxyManager<String> proxyManager;
    private final RedisRateLimitProperties properties;

    /**
     * Cache of bucket configurations per endpoint to avoid re-creating
     * {@link BucketConfiguration}
     * on every request. Configurations are immutable, so this is safe.
     */
    private final Map<String, BucketConfiguration> configCache = new ConcurrentHashMap<>();

    public RateLimiterService(ProxyManager<String> proxyManager, RedisRateLimitProperties properties) {
        this.proxyManager = proxyManager;
        this.properties = properties;
    }

    /**
     * Attempts to consume 1 token from the bucket identified by
     * {@code key + endpoint}.
     * Uses the default capacity and refill parameters from properties.
     *
     * @param key      rate limit identity — typically userId or IP address
     * @param endpoint API path for per-endpoint bucket isolation
     * @return {@code true} if the request is allowed, {@code false} if rate-limited
     */
    public Mono<Boolean> tryConsume(String key, String endpoint) {
        return tryConsume(key, endpoint,
                properties.getDefaultCapacity(),
                properties.getDefaultRefillTokens(),
                properties.getDefaultRefillPeriod());
    }

    /**
     * Attempts to consume 1 token with custom bucket parameters.
     *
     * @param key          rate limit identity
     * @param endpoint     API path for isolation
     * @param capacity     max burst capacity
     * @param refillTokens tokens added per refill period
     * @param refillPeriod duration of one refill cycle
     * @return {@code true} if the request is allowed
     */
    public Mono<Boolean> tryConsume(String key, String endpoint,
            long capacity, long refillTokens, Duration refillPeriod) {
        if (!properties.isEnabled()) {
            return Mono.just(true);
        }

        String bucketKey = buildBucketKey(key, endpoint);
        Supplier<BucketConfiguration> configSupplier = () -> configCache.computeIfAbsent(endpoint,
                ep -> BucketConfiguration.builder()
                        .addLimit(Bandwidth.classic(capacity, Refill.greedy(refillTokens, refillPeriod)))
                        .build());

        return Mono.fromCallable(() -> {
            var bucket = proxyManager.builder().build(bucketKey, configSupplier);
            boolean consumed = bucket.tryConsume(1);
            if (!consumed) {
                log.warn("[rate-limit] Rate limit exceeded for key='{}' endpoint='{}'", key, endpoint);
            }
            return consumed;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private String buildBucketKey(String key, String endpoint) {
        return properties.getKeyPrefix() + key + ":" + endpoint;
    }
}

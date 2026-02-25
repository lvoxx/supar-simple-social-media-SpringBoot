package io.github.lvoxx.redis_starter.service;

import java.util.function.Supplier;

import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Redisson-based distributed lock service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LockService {

    private final RedissonReactiveClient redissonClient;

    @Value("${app.lock.wait-time:3000}")
    private long waitTime;

    @Value("${app.lock.lease-time:10000}")
    private long leaseTime;

    public <T> Mono<T> executeWithLock(String lockKey, Supplier<Mono<T>> operation) {
        log.debug("Acquiring lock: {}", lockKey);

        RLockReactive lock = redissonClient.getLock(lockKey);

        return lock.tryLock(waitTime, leaseTime, java.util.concurrent.TimeUnit.MILLISECONDS)
                .flatMap(acquired -> {
                    if (!acquired) {
                        log.warn("Failed to acquire lock: {}", lockKey);
                        return Mono.error(new RuntimeException("Unable to acquire lock: " + lockKey));
                    }

                    log.debug("Lock acquired: {}", lockKey);

                    return operation.get()
                            .doFinally(signalType -> {
                                lock.unlock()
                                        .doOnSuccess(v -> log.debug("Lock released: {}", lockKey))
                                        .doOnError(
                                                e -> log.error("Error releasing lock {}: {}", lockKey, e.getMessage()))
                                        .subscribe();
                            });
                })
                .doOnError(e -> log.error("Error in lock operation {}: {}", lockKey, e.getMessage()));
    }

    public Mono<Void> executeWithLock(String lockKey, Supplier<Mono<Void>> operation, boolean voidOperation) {
        return executeWithLock(lockKey, operation);
    }
}
package io.github.lvoxx.redis_starter.service;

import java.util.function.Supplier;

import reactor.core.publisher.Mono;

/**
 * Service interface for distributed locking
 */
public interface LockService {

    /**
     * Execute operation with distributed lock
     */
    <T> Mono<T> executeWithLock(String lockKey, Supplier<Mono<T>> operation);

    /**
     * Execute void operation with distributed lock
     */
    Mono<Void> executeWithLock(String lockKey, Supplier<Mono<Void>> operation, boolean voidOperation);
}

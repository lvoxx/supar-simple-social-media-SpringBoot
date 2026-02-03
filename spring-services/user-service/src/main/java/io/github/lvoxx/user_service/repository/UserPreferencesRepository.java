package io.github.lvoxx.user_service.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import io.github.lvoxx.user_service.model.UserPreferences;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for UserPreferences entity
 */
@Repository
public interface UserPreferencesRepository extends R2dbcRepository<UserPreferences, Long> {
    
    /**
     * Find preferences by user ID
     */
    Mono<UserPreferences> findByUserId(Long userId);
    
    /**
     * Delete preferences by user ID
     */
    Mono<Void> deleteByUserId(Long userId);
    
    /**
     * Check if preferences exist for user
     */
    Mono<Boolean> existsByUserId(Long userId);
}
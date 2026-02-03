package io.github.lvoxx.user_service.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import io.github.lvoxx.user_service.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for User entity
 */
@Repository
public interface UserRepository extends R2dbcRepository<User, Long> {

    /**
     * Find user by username
     */
    Mono<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Mono<User> findByEmail(String email);

    /**
     * Find user by Keycloak user ID
     */
    Mono<User> findByKeycloakUserId(String keycloakUserId);

    /**
     * Check if username exists
     */
    Mono<Boolean> existsByUsername(String username);

    /**
     * Check if email exists
     */
    Mono<Boolean> existsByEmail(String email);

    /**
     * Check if Keycloak user ID exists
     */
    Mono<Boolean> existsByKeycloakUserId(String keycloakUserId);

    /**
     * Search users by username (case-insensitive)
     */
    @Query("SELECT * FROM users WHERE LOWER(username) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY created_at DESC LIMIT :limit")
    Flux<User> searchByUsername(String query, int limit);

    /**
     * Search users by display name (case-insensitive)
     */
    @Query("SELECT * FROM users WHERE LOWER(display_name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY created_at DESC LIMIT :limit")
    Flux<User> searchByDisplayName(String query, int limit);

    /**
     * Search users by username or display name
     */
    @Query("SELECT * FROM users WHERE LOWER(username) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(display_name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY created_at DESC LIMIT :limit")
    Flux<User> searchByUsernameOrDisplayName(String query, int limit);

    /**
     * Find verified users
     */
    Flux<User> findByIsVerifiedTrue();

    /**
     * Increment follower count
     */
    @Query("UPDATE users SET follower_count = follower_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = :userId")
    Mono<Void> incrementFollowerCount(Long userId);

    /**
     * Decrement follower count
     */
    @Query("UPDATE users SET follower_count = GREATEST(follower_count - 1, 0), updated_at = CURRENT_TIMESTAMP WHERE id = :userId")
    Mono<Void> decrementFollowerCount(Long userId);

    /**
     * Increment following count
     */
    @Query("UPDATE users SET following_count = following_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = :userId")
    Mono<Void> incrementFollowingCount(Long userId);

    /**
     * Decrement following count
     */
    @Query("UPDATE users SET following_count = GREATEST(following_count - 1, 0), updated_at = CURRENT_TIMESTAMP WHERE id = :userId")
    Mono<Void> decrementFollowingCount(Long userId);

    /**
     * Increment post count
     */
    @Query("UPDATE users SET post_count = post_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = :userId")
    Mono<Void> incrementPostCount(Long userId);

    /**
     * Decrement post count
     */
    @Query("UPDATE users SET post_count = GREATEST(post_count - 1, 0), updated_at = CURRENT_TIMESTAMP WHERE id = :userId")
    Mono<Void> decrementPostCount(Long userId);
}

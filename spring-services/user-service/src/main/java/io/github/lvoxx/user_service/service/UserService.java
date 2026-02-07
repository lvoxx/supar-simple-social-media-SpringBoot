package io.github.lvoxx.user_service.service;

import io.github.lvoxx.user_service.dto.CreateUserRequest;
import io.github.lvoxx.user_service.dto.UpdateUserRequest;
import io.github.lvoxx.user_service.dto.UserDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * UserService implementation with declarative caching via Spring Cache
 * annotations.
 * 
 * Cache strategy:
 * - @Cacheable: Cache the result on read (getUserById, getUserByUsername, etc.)
 * - @CachePut: Update cache after write (updateUser)
 * - @CacheEvict: Invalidate cache on delete or counter updates
 * - Cache name: "users", key: user ID
 */
public interface UserService {

    /**
     * Create a new user
     */
    Mono<UserDTO> createUser(CreateUserRequest request);

    /**
     * Get user by ID
     */
    Mono<UserDTO> getUserById(Long id);

    /**
     * Get user by username
     */
    Mono<UserDTO> getUserByUsername(String username);

    /**
     * Get user by email
     */
    Mono<UserDTO> getUserByEmail(String email);

    /**
     * Get user by Keycloak user ID
     */
    Mono<UserDTO> getUserByKeycloakUserId(String keycloakUserId);

    /**
     * Update user profile
     */
    Mono<UserDTO> updateUser(Long id, UpdateUserRequest request);

    /**
     * Delete user
     */
    Mono<Void> deleteUser(Long id);

    /**
     * Search users by query
     */
    Flux<UserDTO> searchUsers(String query, int limit);

    /**
     * Get verified users
     */
    Flux<UserDTO> getVerifiedUsers();

    /**
     * Increment follower count
     */
    Mono<Void> incrementFollowerCount(Long userId);

    /**
     * Decrement follower count
     */
    Mono<Void> decrementFollowerCount(Long userId);

    /**
     * Increment following count
     */
    Mono<Void> incrementFollowingCount(Long userId);

    /**
     * Decrement following count
     */
    Mono<Void> decrementFollowingCount(Long userId);

    /**
     * Increment post count
     */
    Mono<Void> incrementPostCount(Long userId);

    /**
     * Decrement post count
     */
    Mono<Void> decrementPostCount(Long userId);
}
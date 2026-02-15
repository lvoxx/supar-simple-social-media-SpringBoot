package io.github.lvoxx.post_service.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import io.github.lvoxx.post_service.model.PostShare;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for managing post shares.
 * 
 * <p>
 * Provides reactive database access for post sharing operations:
 * <ul>
 * <li>Check if user has shared a post</li>
 * <li>Find all shares of a specific post</li>
 * <li>Delete a share</li>
 * </ul>
 */
@Repository
public interface PostShareRepository extends R2dbcRepository<PostShare, Long> {

    /**
     * Check if a user has already shared a specific post.
     *
     * @param postId the post ID
     * @param userId the user ID
     * @return Mono emitting true if share exists, false otherwise
     */
    Mono<Boolean> existsByPostIdAndUserId(Long postId, Long userId);

    /**
     * Delete a share by post ID and user ID.
     *
     * @param postId the post ID
     * @param userId the user ID
     * @return Mono that completes when deletion is done
     */
    Mono<Void> deleteByPostIdAndUserId(Long postId, Long userId);

    /**
     * Find all shares for a specific post with pagination.
     *
     * @param postId   the post ID
     * @param pageable pagination information
     * @return Flux of PostShare entities
     */
    Flux<PostShare> findByPostIdOrderByCreatedAtDesc(Long postId, Pageable pageable);

    /**
     * Find all shares by a specific user with pagination.
     *
     * @param userId   the user ID
     * @param pageable pagination information
     * @return Flux of PostShare entities
     */
    Flux<PostShare> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
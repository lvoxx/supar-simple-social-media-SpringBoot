package io.github.lvoxx.post_service.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import io.github.lvoxx.post_service.model.PostBookmark;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for managing post bookmarks.
 * 
 * <p>
 * Provides reactive database access for bookmark operations:
 * <ul>
 * <li>Check if user has bookmarked a post</li>
 * <li>Find bookmarks by user and optionally by collection</li>
 * <li>Delete bookmarks</li>
 * </ul>
 */
@Repository
public interface PostBookmarkRepository extends R2dbcRepository<PostBookmark, Long> {

    /**
     * Check if a user has already bookmarked a specific post.
     *
     * @param postId the post ID
     * @param userId the user ID
     * @return Mono emitting true if bookmark exists, false otherwise
     */
    Mono<Boolean> existsByPostIdAndUserId(Long postId, Long userId);

    /**
     * Delete a bookmark by post ID and user ID.
     *
     * @param postId the post ID
     * @param userId the user ID
     * @return Mono that completes when deletion is done
     */
    Mono<Void> deleteByPostIdAndUserId(Long postId, Long userId);

    /**
     * Find all bookmarks for a specific user with pagination.
     *
     * @param userId   the user ID
     * @param pageable pagination information
     * @return Flux of PostBookmark entities ordered by creation date descending
     */
    Flux<PostBookmark> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find bookmarks in a specific collection for a user.
     *
     * @param userId         the user ID
     * @param collectionName the collection name
     * @param pageable       pagination information
     * @return Flux of PostBookmark entities in the specified collection
     */
    Flux<PostBookmark> findByUserIdAndCollectionNameOrderByCreatedAtDesc(
            Long userId, String collectionName, Pageable pageable);

    /**
     * Find all distinct collection names for a user.
     * Useful for displaying user's bookmark collections.
     *
     * @param userId the user ID
     * @return Flux of collection names
     */
    Flux<String> findDistinctCollectionNameByUserId(Long userId);
}
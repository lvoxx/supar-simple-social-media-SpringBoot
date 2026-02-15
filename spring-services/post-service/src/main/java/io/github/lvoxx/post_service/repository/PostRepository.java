package io.github.lvoxx.post_service.repository;


import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import io.github.lvoxx.post_service.model.Post;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for managing posts in the database.
 * 
 * <p>Provides reactive database access for post CRUD operations and custom queries.
 * All operations are non-blocking and return reactive types (Mono/Flux).
 * 
 * <p>Key features:
 * <ul>
 *   <li>Basic CRUD operations via R2dbcRepository</li>
 *   <li>Custom queries for filtering and pagination</li>
 *   <li>Counter management (likes, comments, shares, bookmarks, views)</li>
 *   <li>Soft delete and archive operations</li>
 * </ul>
 *
 * @see Post
 * @see org.springframework.data.r2dbc.repository.R2dbcRepository
 */
@Repository
public interface PostRepository extends R2dbcRepository<Post, Long> {

    /**
     * Find all non-deleted, non-archived posts by a specific user.
     * Results are ordered by creation date descending (newest first).
     *
     * @param userId the user ID
     * @param pageable pagination and sorting information
     * @return Flux of active posts by the user
     */
    @Query("SELECT * FROM posts WHERE user_id = :userId AND is_deleted = false AND is_archived = false ORDER BY created_at DESC")
    Flux<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find all public, non-deleted, non-archived posts.
     * Results are ordered by creation date descending (newest first).
     * This is typically used for the public feed.
     *
     * @param pageable pagination and sorting information
     * @return Flux of public posts
     */
    @Query("SELECT * FROM posts WHERE visibility = 'PUBLIC' AND is_deleted = false AND is_archived = false ORDER BY created_at DESC")
    Flux<Post> findPublicPostsOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find all archived posts by a specific user.
     * Results are ordered by archive date descending.
     *
     * @param userId the user ID
     * @param pageable pagination and sorting information
     * @return Flux of archived posts
     */
    @Query("SELECT * FROM posts WHERE user_id = :userId AND is_archived = true AND is_deleted = false ORDER BY archived_at DESC")
    Flux<Post> findArchivedPostsByUserId(Long userId, Pageable pageable);

    /**
     * Count the total number of active (non-deleted, non-archived) posts by a user.
     *
     * @param userId the user ID
     * @return Mono emitting the count
     */
    @Query("SELECT COUNT(*) FROM posts WHERE user_id = :userId AND is_deleted = false AND is_archived = false")
    Mono<Long> countByUserId(Long userId);

    /**
     * Increment the like count for a post.
     * Uses database stored procedure for atomic operation.
     *
     * @param postId the post ID
     * @return Mono that completes when operation is done
     */
    @Query("SELECT increment_post_like_count(:postId)")
    Mono<Void> incrementLikeCount(Long postId);

    /**
     * Decrement the like count for a post.
     * Uses database stored procedure for atomic operation.
     * Count will not go below zero.
     *
     * @param postId the post ID
     * @return Mono that completes when operation is done
     */
    @Query("SELECT decrement_post_like_count(:postId)")
    Mono<Void> decrementLikeCount(Long postId);

    /**
     * Increment the comment count for a post.
     * Called when a new comment is added to the post.
     *
     * @param postId the post ID
     * @return Mono that completes when operation is done
     */
    @Query("SELECT increment_post_comment_count(:postId)")
    Mono<Void> incrementCommentCount(Long postId);

    /**
     * Decrement the comment count for a post.
     * Called when a comment is deleted from the post.
     * Count will not go below zero.
     *
     * @param postId the post ID
     * @return Mono that completes when operation is done
     */
    @Query("SELECT decrement_post_comment_count(:postId)")
    Mono<Void> decrementCommentCount(Long postId);

    /**
     * Increment the share count for a post.
     * Called when a user shares the post.
     *
     * @param postId the post ID
     * @return Mono that completes when operation is done
     */
    @Query("SELECT increment_post_share_count(:postId)")
    Mono<Void> incrementShareCount(Long postId);

    /**
     * Decrement the share count for a post.
     * Called when a user removes their share.
     * Count will not go below zero.
     *
     * @param postId the post ID
     * @return Mono that completes when operation is done
     */
    @Query("SELECT decrement_post_share_count(:postId)")
    Mono<Void> decrementShareCount(Long postId);

    /**
     * Increment the bookmark count for a post.
     * Called when a user bookmarks the post.
     *
     * @param postId the post ID
     * @return Mono that completes when operation is done
     */
    @Query("SELECT increment_post_bookmark_count(:postId)")
    Mono<Void> incrementBookmarkCount(Long postId);

    /**
     * Decrement the bookmark count for a post.
     * Called when a user removes their bookmark.
     * Count will not go below zero.
     *
     * @param postId the post ID
     * @return Mono that completes when operation is done
     */
    @Query("SELECT decrement_post_bookmark_count(:postId)")
    Mono<Void> decrementBookmarkCount(Long postId);

    /**
     * Increment the view count for a post.
     * Called when a user views the post.
     * Note: This does NOT update the updated_at timestamp.
     *
     * @param postId the post ID
     * @return Mono that completes when operation is done
     */
    @Query("SELECT increment_post_view_count(:postId)")
    Mono<Void> incrementViewCount(Long postId);

    /**
     * Soft delete a post.
     * Sets is_deleted to true and records deletion timestamp.
     * The post remains in database but is filtered out from queries.
     *
     * @param postId the post ID
     * @return Mono that completes when operation is done
     */
    @Query("SELECT soft_delete_post(:postId)")
    Mono<Void> softDelete(Long postId);

    /**
     * Archive a post.
     * Sets is_archived to true and records archive timestamp.
     * Archived posts are hidden from main feed but accessible in archive view.
     *
     * @param postId the post ID
     * @return Mono that completes when operation is done
     */
    @Query("SELECT archive_post(:postId)")
    Mono<Void> archivePost(Long postId);

    /**
     * Unarchive a post.
     * Sets is_archived to false and clears archive timestamp.
     * Post becomes visible in main feed again.
     *
     * @param postId the post ID
     * @return Mono that completes when operation is done
     */
    @Query("SELECT unarchive_post(:postId)")
    Mono<Void> unarchivePost(Long postId);
}
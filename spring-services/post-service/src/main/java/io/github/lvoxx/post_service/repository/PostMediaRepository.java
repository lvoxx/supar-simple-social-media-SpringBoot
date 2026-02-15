package io.github.lvoxx.post_service.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import io.github.lvoxx.post_service.model.PostMedia;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for managing post media attachments.
 * 
 * <p>
 * Handles database operations for images and videos attached to posts.
 * Media files are stored in Cloudinary, and this repository manages the
 * metadata.
 * 
 * <p>
 * Key operations:
 * <ul>
 * <li>Find all media for a post (ordered by display order)</li>
 * <li>Delete all media when post is deleted</li>
 * </ul>
 *
 * @see PostMedia
 */
@Repository
public interface PostMediaRepository extends R2dbcRepository<PostMedia, Long> {

    /**
     * Find all media attachments for a specific post.
     * Results are ordered by display_order to maintain the sequence of media.
     *
     * @param postId the post ID
     * @return Flux of PostMedia entities ordered by display order
     */
    Flux<PostMedia> findByPostIdOrderByDisplayOrder(Long postId);

    /**
     * Delete all media attachments for a specific post.
     * Used when a post is deleted. Note: This only deletes the database records.
     * The actual media files in Cloudinary should be deleted separately.
     *
     * @param postId the post ID
     * @return Mono that completes when deletion is done
     */
    Mono<Void> deleteByPostId(Long postId);
}
package io.github.lvoxx.post_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lvoxx.cloudinary_starter.dto.MediaResponse;
import io.github.lvoxx.cloudinary_starter.dto.MediaUploadRequest;
import io.github.lvoxx.common_core.exception.model.post_exception.ContentViolationException;
import io.github.lvoxx.common_core.exception.model.post_exception.MediaUploadFailedException;
import io.github.lvoxx.common_core.exception.model.post_exception.PostDeletedException;
import io.github.lvoxx.common_core.exception.model.post_exception.PostNotFoundException;
import io.github.lvoxx.post_service.dto.CreatePostRequest;
import io.github.lvoxx.post_service.dto.PostResponse;
import io.github.lvoxx.post_service.dto.SharePostRequest;
import io.github.lvoxx.post_service.dto.UpdatePostRequest;
import io.github.lvoxx.post_service.elasticsearch.model.PostDocument;
import io.github.lvoxx.post_service.guard.PostGuardClient;
import io.github.lvoxx.post_service.model.Post;
import io.github.lvoxx.post_service.model.PostBookmark;
import io.github.lvoxx.post_service.model.PostMedia;
import io.github.lvoxx.post_service.model.PostShare;
import io.github.lvoxx.post_service.repository.PostBookmarkRepository;
import io.github.lvoxx.post_service.repository.PostMediaRepository;
import io.github.lvoxx.post_service.repository.PostRepository;
import io.github.lvoxx.post_service.repository.PostShareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service layer for managing posts in the social media platform.
 * 
 * <p>
 * This service provides comprehensive post management functionality including:
 * <ul>
 * <li><b>Post CRUD</b>: Create, read, update, and soft delete posts</li>
 * <li><b>Media Management</b>: Upload and manage images/videos via
 * Cloudinary</li>
 * <li><b>Visibility Control</b>: PUBLIC, FOLLOWERS, PRIVATE, CUSTOM visibility
 * levels</li>
 * <li><b>Comment Permissions</b>: Control who can comment (EVERYONE, FOLLOWERS,
 * MENTIONED, NOBODY)</li>
 * <li><b>Sharing</b>: Share posts with optional commentary</li>
 * <li><b>Bookmarking</b>: Save posts for later reference with collections</li>
 * <li><b>Archiving</b>: Archive/unarchive posts</li>
 * <li><b>Content Validation</b>: AI-powered content validation via
 * post-guard-service</li>
 * <li><b>Event Streaming</b>: Kafka events for notifications</li>
 * <li><b>Search Sync</b>: Elasticsearch synchronization for search
 * functionality</li>
 * <li><b>Caching</b>: Redis caching with automatic eviction</li>
 * </ul>
 * 
 * <p>
 * <b>Important Notes:</b>
 * <ul>
 * <li>All operations are reactive (non-blocking) using Project Reactor</li>
 * <li>Like and comment operations are handled by separate microservices</li>
 * <li>Counters (likes, comments) are updated via repository methods called by
 * other services</li>
 * <li>Distributed locks (Redisson) are used for share and bookmark
 * operations</li>
 * <li>Posts are soft-deleted (is_deleted flag) not physically removed</li>
 * </ul>
 *
 * @see Post
 * @see PostRepository
 * @see PostMediaService
 * @see PostGuardClient
 * @author Post Service Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final PostShareRepository postShareRepository;
    private final PostBookmarkRepository postBookmarkRepository;
    private final PostMediaService postMediaService;
    private final PostGuardClient postGuardClient;
    private final PostEventProducer postEventProducer;
    private final ElasticsearchSyncProducer elasticsearchSyncProducer;
    private final RedissonClient redissonClient;

    /**
     * Creates a new post with optional media attachments.
     * 
     * <p>
     * This method performs the following operations:
     * <ol>
     * <li>Validates content using AI (post-guard-service)</li>
     * <li>Creates post entity in database</li>
     * <li>Uploads media files to Cloudinary (if any)</li>
     * <li>Sends POST_CREATED event to Kafka</li>
     * <li>Syncs post to Elasticsearch for search</li>
     * </ol>
     * 
     * <p>
     * <b>Content Validation:</b>
     * Content is validated against community guidelines using an AI model.
     * If content violates guidelines, a ContentViolationException is thrown.
     * 
     * <p>
     * <b>Media Upload:</b>
     * Media files are uploaded to Cloudinary in parallel. If any upload fails,
     * the entire operation is rolled back (transactional).
     * 
     * @param userId  the ID of the user creating the post (from X-User-Id header)
     * @param request the post creation request containing content, visibility, and
     *                media
     * @return Mono emitting the created PostResponse with HATEOAS links
     * @throws ContentViolationException  if content violates community guidelines
     * @throws MediaUploadFailedException if media upload fails
     * @see CreatePostRequest
     * @see PostResponse
     */
    @Transactional
    public Mono<PostResponse> createPost(Long userId, CreatePostRequest request) {
        log.info("Creating post for userId: {}", userId);

        return postGuardClient.validateOrThrow(request.getContent())
                .then(Mono.defer(() -> {
                    Post post = Post.builder()
                            .userId(userId)
                            .content(request.getContent())
                            .visibility(request.getVisibility())
                            .commentsEnabled(request.getCommentsEnabled())
                            .allowedCommenters(request.getAllowedCommenters())
                            .sharingEnabled(request.getSharingEnabled())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    return postRepository.save(post);
                }))
                .flatMap(savedPost -> {
                    if (request.getMedia() != null && !request.getMedia().isEmpty()) {
                        return uploadMedia(savedPost.getId(), request.getMedia())
                                .collectList()
                                .map(mediaList -> {
                                    savedPost.setMedia(mediaList);
                                    return savedPost;
                                });
                    }
                    return Mono.just(savedPost);
                })
                .flatMap(post -> {
                    PostEvent event = PostEvent.builder()
                            .postId(post.getId())
                            .userId(post.getUserId())
                            .eventType(PostEvent.EventType.POST_CREATED)
                            .content(post.getContent())
                            .timestamp(LocalDateTime.now())
                            .build();

                    return postEventProducer.sendPostEvent(event)
                            .then(syncToElasticsearch(post))
                            .thenReturn(post);
                })
                .map(this::mapToResponse)
                .doOnSuccess(response -> log.info("Post created successfully: postId={}", response.getId()))
                .doOnError(error -> log.error("Failed to create post for userId={}: {}", userId, error.getMessage()));
    }

    /**
     * Retrieves a post by its ID.
     * 
     * <p>
     * This method:
     * <ol>
     * <li>Fetches the post from database (or cache if available)</li>
     * <li>Loads associated media attachments</li>
     * <li>Checks if current user has bookmarked/shared the post</li>
     * <li>Increments view count (async, doesn't block response)</li>
     * </ol>
     * 
     * <p>
     * <b>Caching:</b>
     * Posts are cached in Redis with key "posts:{postId}" for 1 hour.
     * Cache is automatically evicted when post is updated or deleted.
     * 
     * <p>
     * <b>Deleted Posts:</b>
     * Throws PostDeletedException if post is soft-deleted.
     * 
     * @param postId        the ID of the post to retrieve
     * @param currentUserId the ID of the current user (nullable, from X-User-Id
     *                      header)
     * @return Mono emitting the PostResponse
     * @throws PostNotFoundException if post doesn't exist
     * @throws PostDeletedException  if post is deleted
     * @see PostResponse
     */
    @Cacheable(value = "posts", key = "#postId")
    public Mono<PostResponse> getPostById(Long postId, Long currentUserId) {
        log.debug("Getting post: postId={}", postId);

        return postRepository.findById(postId)
                .switchIfEmpty(Mono.error(new PostNotFoundException(postId)))
                .flatMap(post -> {
                    if (Boolean.TRUE.equals(post.getIsDeleted())) {
                        return Mono.error(new PostDeletedException(postId));
                    }
                    return loadPostMedia(post);
                })
                .flatMap(post -> checkUserInteractions(post, currentUserId))
                .doOnNext(post -> postRepository.incrementViewCount(post.getId()).subscribe())
                .map(this::mapToResponse)
                .doOnError(error -> log.error("Failed to get post {}: {}", postId, error.getMessage()));
    }

    /**
     * Retrieves all active posts by a specific user.
     * 
     * <p>
     * Returns only non-deleted, non-archived posts.
     * Results are paginated and ordered by creation date (newest first).
     * 
     * @param userId        the ID of the user whose posts to retrieve
     * @param page          the page number (0-indexed)
     * @param size          the number of posts per page
     * @param currentUserId the ID of the current user (nullable)
     * @return Flux of PostResponse objects
     * @see PostResponse
     */
    public Flux<PostResponse> getPostsByUser(Long userId, int page, int size, Long currentUserId) {
        log.debug("Getting posts for userId: {}, page: {}, size: {}", userId, page, size);

        return postRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .flatMap(this::loadPostMedia)
                .flatMap(post -> checkUserInteractions(post, currentUserId))
                .map(this::mapToResponse);
    }

    /**
     * Retrieves all public posts for the main feed.
     * 
     * <p>
     * Returns only PUBLIC, non-deleted, non-archived posts.
     * Results are paginated and ordered by creation date (newest first).
     * 
     * @param page          the page number (0-indexed)
     * @param size          the number of posts per page
     * @param currentUserId the ID of the current user (nullable)
     * @return Flux of PostResponse objects
     * @see PostResponse
     */
    public Flux<PostResponse> getPublicPosts(int page, int size, Long currentUserId) {
        log.debug("Getting public posts: page={}, size={}", page, size);

        return postRepository.findPublicPostsOrderByCreatedAtDesc(PageRequest.of(page, size))
                .flatMap(this::loadPostMedia)
                .flatMap(post -> checkUserInteractions(post, currentUserId))
                .map(this::mapToResponse);
    }

    /**
     * Retrieves archived posts for a user.
     * 
     * <p>
     * Returns only archived (but not deleted) posts.
     * Results are paginated and ordered by archive date (newest first).
     * 
     * @param userId        the ID of the user whose archived posts to retrieve
     * @param page          the page number (0-indexed)
     * @param size          the number of posts per page
     * @param currentUserId the ID of the current user (nullable)
     * @return Flux of PostResponse objects
     * @see PostResponse
     */
    public Flux<PostResponse> getArchivedPosts(Long userId, int page, int size, Long currentUserId) {
        log.debug("Getting archived posts for userId: {}", userId);

        return postRepository.findArchivedPostsByUserId(userId, PageRequest.of(page, size))
                .flatMap(this::loadPostMedia)
                .flatMap(post -> checkUserInteractions(post, currentUserId))
                .map(this::mapToResponse);
    }

    /**
     * Updates an existing post.
     * 
     * <p>
     * This method:
     * <ol>
     * <li>Verifies post exists and user is the owner</li>
     * <li>Validates new content if provided (via AI)</li>
     * <li>Updates allowed fields</li>
     * <li>Sends POST_UPDATED event to Kafka</li>
     * <li>Syncs changes to Elasticsearch</li>
     * <li>Evicts cache</li>
     * </ol>
     * 
     * <p>
     * <b>Updatable Fields:</b>
     * <ul>
     * <li>content - Post text content</li>
     * <li>visibility - PUBLIC, FOLLOWERS, PRIVATE, CUSTOM</li>
     * <li>commentsEnabled - Enable/disable comments</li>
     * <li>allowedCommenters - Who can comment</li>
     * <li>sharingEnabled - Enable/disable sharing</li>
     * </ul>
     * 
     * @param postId  the ID of the post to update
     * @param userId  the ID of the user making the update
     * @param request the update request containing fields to change
     * @return Mono emitting the updated PostResponse
     * @throws PostNotFoundException     if post doesn't exist
     * @throws RuntimeException          if user is not the owner
     * @throws ContentViolationException if new content violates guidelines
     * @see UpdatePostRequest
     * @see PostResponse
     */
    @Transactional
    @CacheEvict(value = "posts", key = "#postId")
    public Mono<PostResponse> updatePost(Long postId, Long userId, UpdatePostRequest request) {
        log.info("Updating post: postId={}, userId={}", postId, userId);

        return postRepository.findById(postId)
                .switchIfEmpty(Mono.error(new PostNotFoundException(postId)))
                .flatMap(post -> {
                    if (!post.getUserId().equals(userId)) {
                        return Mono.error(new RuntimeException("Unauthorized: You can only update your own posts"));
                    }

                    if (request.getContent() != null) {
                        return postGuardClient.validateOrThrow(request.getContent())
                                .thenReturn(post);
                    }
                    return Mono.just(post);
                })
                .map(post -> {
                    if (request.getContent() != null) {
                        post.setContent(request.getContent());
                    }
                    if (request.getVisibility() != null) {
                        post.setVisibility(request.getVisibility());
                    }
                    if (request.getCommentsEnabled() != null) {
                        post.setCommentsEnabled(request.getCommentsEnabled());
                    }
                    if (request.getAllowedCommenters() != null) {
                        post.setAllowedCommenters(request.getAllowedCommenters());
                    }
                    if (request.getSharingEnabled() != null) {
                        post.setSharingEnabled(request.getSharingEnabled());
                    }
                    return post;
                })
                .flatMap(postRepository::save)
                .flatMap(post -> {
                    PostEvent event = PostEvent.builder()
                            .postId(post.getId())
                            .userId(post.getUserId())
                            .eventType(PostEvent.EventType.POST_UPDATED)
                            .timestamp(LocalDateTime.now())
                            .build();

                    return postEventProducer.sendPostEvent(event)
                            .then(syncToElasticsearch(post))
                            .thenReturn(post);
                })
                .flatMap(this::loadPostMedia)
                .flatMap(post -> checkUserInteractions(post, userId))
                .map(this::mapToResponse)
                .doOnSuccess(response -> log.info("Post updated successfully: postId={}", postId))
                .doOnError(error -> log.error("Failed to update post {}: {}", postId, error.getMessage()));
    }

    /**
     * Soft deletes a post.
     * 
     * <p>
     * Sets is_deleted flag to true and records deletion timestamp.
     * The post remains in database but is filtered from all queries.
     * Media files are also deleted from Cloudinary.
     * 
     * <p>
     * <b>Note:</b> This is not a hard delete. The post can potentially
     * be restored by database administrators if needed.
     * 
     * @param postId the ID of the post to delete
     * @param userId the ID of the user requesting deletion
     * @return Mono that completes when deletion is done
     * @throws PostNotFoundException if post doesn't exist
     * @throws RuntimeException      if user is not the owner
     */
    @Transactional
    @CacheEvict(value = "posts", key = "#postId")
    public Mono<Void> deletePost(Long postId, Long userId) {
        log.info("Deleting post: postId={}, userId={}", postId, userId);

        return postRepository.findById(postId)
                .switchIfEmpty(Mono.error(new PostNotFoundException(postId)))
                .flatMap(post -> {
                    if (!post.getUserId().equals(userId)) {
                        return Mono.error(new RuntimeException("Unauthorized: You can only delete your own posts"));
                    }

                    // Delete media from Cloudinary
                    return postMediaRepository.findByPostIdOrderByDisplayOrder(postId)
                            .flatMap(media -> postMediaService.deleteMedia(media.getPublicId()))
                            .then(postRepository.softDelete(postId));
                })
                .then(Mono.defer(() -> {
                    PostEvent event = PostEvent.builder()
                            .postId(postId)
                            .userId(userId)
                            .eventType(PostEvent.EventType.POST_DELETED)
                            .timestamp(LocalDateTime.now())
                            .build();

                    return postEventProducer.sendPostEvent(event);
                }))
                .doOnSuccess(v -> log.info("Post deleted successfully: postId={}", postId))
                .doOnError(error -> log.error("Failed to delete post {}: {}", postId, error.getMessage()));
    }

    /**
     * Archives a post.
     * 
     * <p>
     * Sets is_archived flag to true. Archived posts are hidden from
     * main feed but accessible via getArchivedPosts() method.
     * 
     * <p>
     * Useful for users who want to hide old posts without deleting them.
     * 
     * @param postId the ID of the post to archive
     * @param userId the ID of the user requesting archival
     * @return Mono that completes when archival is done
     * @throws PostNotFoundException if post doesn't exist
     * @throws RuntimeException      if user is not the owner
     */
    @Transactional
    @CacheEvict(value = "posts", key = "#postId")
    public Mono<Void> archivePost(Long postId, Long userId) {
        log.info("Archiving post: postId={}, userId={}", postId, userId);

        return postRepository.findById(postId)
                .switchIfEmpty(Mono.error(new PostNotFoundException(postId)))
                .flatMap(post -> {
                    if (!post.getUserId().equals(userId)) {
                        return Mono.error(new RuntimeException("Unauthorized"));
                    }
                    return postRepository.archivePost(postId);
                })
                .doOnSuccess(v -> log.info("Post archived: postId={}", postId));
    }

    /**
     * Unarchives a post.
     * 
     * <p>
     * Sets is_archived flag to false. Post becomes visible in main feed again.
     * 
     * @param postId the ID of the post to unarchive
     * @param userId the ID of the user requesting unarchival
     * @return Mono that completes when unarchival is done
     * @throws PostNotFoundException if post doesn't exist
     * @throws RuntimeException      if user is not the owner
     */
    @Transactional
    @CacheEvict(value = "posts", key = "#postId")
    public Mono<Void> unarchivePost(Long postId, Long userId) {
        log.info("Unarchiving post: postId={}, userId={}", postId, userId);

        return postRepository.findById(postId)
                .switchIfEmpty(Mono.error(new PostNotFoundException(postId)))
                .flatMap(post -> {
                    if (!post.getUserId().equals(userId)) {
                        return Mono.error(new RuntimeException("Unauthorized"));
                    }
                    return postRepository.unarchivePost(postId);
                })
                .doOnSuccess(v -> log.info("Post unarchived: postId={}", postId));
    }

    /**
     * Shares a post with optional user commentary.
     * 
     * <p>
     * Sharing allows users to repost content to their followers with
     * optional additional commentary. Uses distributed locking to prevent
     * duplicate shares in concurrent requests.
     * 
     * <p>
     * <b>Distributed Lock:</b>
     * A Redisson distributed lock is acquired to ensure atomicity.
     * Lock timeout: 10 seconds, Wait time: 5 seconds.
     * 
     * @param postId  the ID of the post to share
     * @param userId  the ID of the user sharing the post
     * @param request the share request containing optional commentary
     * @return Mono that completes when sharing is done
     * @throws PostNotFoundException if post doesn't exist
     * @throws RuntimeException      if post has sharing disabled
     * @throws RuntimeException      if unable to acquire lock
     */
    @Transactional
    @CacheEvict(value = "posts", key = "#postId")
    public Mono<Void> sharePost(Long postId, Long userId, SharePostRequest request) {
        log.info("Sharing post: postId={}, userId={}", postId, userId);

        String lockKey = "post:share:" + postId + ":" + userId;
        RLock lock = redissonClient.getLock(lockKey);

        return Mono.fromCallable(() -> {
            try {
                return lock.tryLock(5, 10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        })
                .flatMap(locked -> {
                    if (!locked) {
                        return Mono.error(new RuntimeException("Could not acquire lock for sharing"));
                    }

                    return postRepository.findById(postId)
                            .switchIfEmpty(Mono.error(new PostNotFoundException(postId)))
                            .flatMap(post -> {
                                if (Boolean.FALSE.equals(post.getSharingEnabled())) {
                                    return Mono.error(new RuntimeException("Sharing is disabled for this post"));
                                }
                                return postShareRepository.existsByPostIdAndUserId(postId, userId);
                            })
                            .flatMap(exists -> {
                                if (Boolean.TRUE.equals(exists)) {
                                    return Mono.empty(); // Already shared
                                }

                                PostShare share = PostShare.builder()
                                        .postId(postId)
                                        .userId(userId)
                                        .sharedContent(request.getSharedContent())
                                        .createdAt(LocalDateTime.now())
                                        .build();

                                return postShareRepository.save(share)
                                        .then(postRepository.incrementShareCount(postId))
                                        .then(Mono.defer(() -> {
                                            PostEvent event = PostEvent.builder()
                                                    .postId(postId)
                                                    .userId(userId)
                                                    .eventType(PostEvent.EventType.POST_SHARED)
                                                    .timestamp(LocalDateTime.now())
                                                    .build();
                                            return postEventProducer.sendPostEvent(event);
                                        }));
                            })
                            .doFinally(signalType -> lock.unlock());
                })
                .doOnSuccess(v -> log.info("Post shared: postId={}, userId={}", postId, userId))
                .doOnError(error -> log.error("Failed to share post {}: {}", postId, error.getMessage()));
    }

    /**
     * Removes a user's share of a post.
     * 
     * <p>
     * Uses distributed locking to ensure atomicity.
     * 
     * @param postId the ID of the post to unshare
     * @param userId the ID of the user removing their share
     * @return Mono that completes when unsharing is done
     */
    @Transactional
    @CacheEvict(value = "posts", key = "#postId")
    public Mono<Void> unsharePost(Long postId, Long userId) {
        log.info("Unsharing post: postId={}, userId={}", postId, userId);

        String lockKey = "post:share:" + postId + ":" + userId;
        RLock lock = redissonClient.getLock(lockKey);

        return Mono.fromCallable(() -> {
            try {
                return lock.tryLock(5, 10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        })
                .flatMap(locked -> {
                    if (!locked) {
                        return Mono.error(new RuntimeException("Could not acquire lock"));
                    }

                    return postShareRepository.existsByPostIdAndUserId(postId, userId)
                            .flatMap(exists -> {
                                if (Boolean.FALSE.equals(exists)) {
                                    return Mono.empty(); // Not shared
                                }

                                return postShareRepository.deleteByPostIdAndUserId(postId, userId)
                                        .then(postRepository.decrementShareCount(postId));
                            })
                            .doFinally(signalType -> lock.unlock());
                })
                .doOnSuccess(v -> log.info("Post unshared: postId={}, userId={}", postId, userId));
    }

    /**
     * Bookmarks a post for later reference.
     * 
     * <p>
     * Users can optionally specify a collection name to organize bookmarks.
     * Uses distributed locking to prevent duplicate bookmarks.
     * 
     * @param postId         the ID of the post to bookmark
     * @param userId         the ID of the user bookmarking
     * @param collectionName optional collection name (can be null)
     * @return Mono that completes when bookmarking is done
     * @throws PostNotFoundException if post doesn't exist
     */
    @Transactional
    @CacheEvict(value = "posts", key = "#postId")
    public Mono<Void> bookmarkPost(Long postId, Long userId, String collectionName) {
        log.info("Bookmarking post: postId={}, userId={}, collection={}", postId, userId, collectionName);

        String lockKey = "post:bookmark:" + postId + ":" + userId;
        RLock lock = redissonClient.getLock(lockKey);

        return Mono.fromCallable(() -> {
            try {
                return lock.tryLock(5, 10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        })
                .flatMap(locked -> {
                    if (!locked) {
                        return Mono.error(new RuntimeException("Could not acquire lock"));
                    }

                    return postRepository.findById(postId)
                            .switchIfEmpty(Mono.error(new PostNotFoundException(postId)))
                            .then(postBookmarkRepository.existsByPostIdAndUserId(postId, userId))
                            .flatMap(exists -> {
                                if (Boolean.TRUE.equals(exists)) {
                                    return Mono.empty(); // Already bookmarked
                                }

                                PostBookmark bookmark = PostBookmark.builder()
                                        .postId(postId)
                                        .userId(userId)
                                        .collectionName(collectionName)
                                        .createdAt(LocalDateTime.now())
                                        .build();

                                return postBookmarkRepository.save(bookmark)
                                        .then(postRepository.incrementBookmarkCount(postId));
                            })
                            .doFinally(signalType -> lock.unlock());
                })
                .doOnSuccess(v -> log.info("Post bookmarked: postId={}", postId));
    }

    /**
     * Removes a bookmark from a post.
     * 
     * @param postId the ID of the post to unbookmark
     * @param userId the ID of the user removing the bookmark
     * @return Mono that completes when unbookmarking is done
     */
    @Transactional
    @CacheEvict(value = "posts", key = "#postId")
    public Mono<Void> unbookmarkPost(Long postId, Long userId) {
        log.info("Unbookmarking post: postId={}, userId={}", postId, userId);

        String lockKey = "post:bookmark:" + postId + ":" + userId;
        RLock lock = redissonClient.getLock(lockKey);

        return Mono.fromCallable(() -> {
            try {
                return lock.tryLock(5, 10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        })
                .flatMap(locked -> {
                    if (!locked) {
                        return Mono.error(new RuntimeException("Could not acquire lock"));
                    }

                    return postBookmarkRepository.existsByPostIdAndUserId(postId, userId)
                            .flatMap(exists -> {
                                if (Boolean.FALSE.equals(exists)) {
                                    return Mono.empty(); // Not bookmarked
                                }

                                return postBookmarkRepository.deleteByPostIdAndUserId(postId, userId)
                                        .then(postRepository.decrementBookmarkCount(postId));
                            })
                            .doFinally(signalType -> lock.unlock());
                })
                .doOnSuccess(v -> log.info("Post unbookmarked: postId={}", postId));
    }

    /**
     * Retrieves all bookmarked posts for a user.
     * 
     * @param userId        the user ID
     * @param page          page number
     * @param size          page size
     * @param currentUserId current user ID (for interaction checks)
     * @return Flux of bookmarked posts
     */
    public Flux<PostResponse> getBookmarkedPosts(Long userId, int page, int size, Long currentUserId) {
        log.debug("Getting bookmarks for userId: {}", userId);

        return postBookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .flatMap(bookmark -> postRepository.findById(bookmark.getPostId()))
                .flatMap(this::loadPostMedia)
                .flatMap(post -> checkUserInteractions(post, currentUserId))
                .map(this::mapToResponse);
    }

    /**
     * Retrieves bookmarked posts in a specific collection.
     * 
     * @param userId         the user ID
     * @param collectionName the collection name
     * @param page           page number
     * @param size           page size
     * @param currentUserId  current user ID
     * @return Flux of bookmarked posts in the collection
     */
    public Flux<PostResponse> getBookmarksByCollection(Long userId, String collectionName,
            int page, int size, Long currentUserId) {
        log.debug("Getting bookmarks in collection '{}' for userId: {}", collectionName, userId);

        return postBookmarkRepository.findByUserIdAndCollectionNameOrderByCreatedAtDesc(
                userId, collectionName, PageRequest.of(page, size))
                .flatMap(bookmark -> postRepository.findById(bookmark.getPostId()))
                .flatMap(this::loadPostMedia)
                .flatMap(post -> checkUserInteractions(post, currentUserId))
                .map(this::mapToResponse);
    }

    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================

    /**
     * Uploads multiple media files to Cloudinary in parallel.
     * 
     * <p>
     * Handles both images and videos. Each upload is processed independently.
     * If any upload fails, the error is propagated and the transaction is rolled
     * back.
     * 
     * @param postId        the post ID to associate media with
     * @param mediaRequests list of media upload requests
     * @return Flux of successfully uploaded PostMedia entities
     */
    private Flux<PostMedia> uploadMedia(Long postId, List<MediaUploadRequest> mediaRequests) {
        return Flux.fromIterable(mediaRequests)
                .flatMap(request -> {
                    if (request.getMediaType() == PostMedia.MediaType.IMAGE) {
                        return postMediaService.uploadPostImage(postId, request);
                    } else {
                        return postMediaService.uploadPostVideo(postId, request);
                    }
                })
                .flatMap(postMediaRepository::save);
    }

    /**
     * Loads media attachments for a post.
     * 
     * <p>
     * Fetches all PostMedia entities associated with the post
     * and attaches them to the Post entity's transient media list.
     * 
     * @param post the post entity
     * @return Mono of Post with loaded media
     */
    private Mono<Post> loadPostMedia(Post post) {
        return postMediaRepository.findByPostIdOrderByDisplayOrder(post.getId())
                .collectList()
                .map(mediaList -> {
                    post.setMedia(mediaList);
                    return post;
                });
    }

    /**
     * Checks user interactions (bookmark, share) for the current user.
     * 
     * <p>
     * Sets transient fields on Post entity:
     * <ul>
     * <li>isBookmarkedByCurrentUser</li>
     * <li>isSharedByCurrentUser</li>
     * </ul>
     * 
     * <p>
     * If currentUserId is null, both fields are set to false.
     * 
     * @param post   the post entity
     * @param userId the current user ID (nullable)
     * @return Mono of Post with interaction flags set
     */
    private Mono<Post> checkUserInteractions(Post post, Long userId) {
        if (userId == null) {
            post.setIsBookmarkedByCurrentUser(false);
            post.setIsSharedByCurrentUser(false);
            return Mono.just(post);
        }

        Mono<Boolean> isBookmarked = postBookmarkRepository.existsByPostIdAndUserId(post.getId(), userId);
        Mono<Boolean> isShared = postShareRepository.existsByPostIdAndUserId(post.getId(), userId);

        return Mono.zip(isBookmarked, isShared)
                .map(tuple -> {
                    post.setIsBookmarkedByCurrentUser(tuple.getT1());
                    post.setIsSharedByCurrentUser(tuple.getT2());
                    return post;
                });
    }

    /**
     * Syncs a post to Elasticsearch for search functionality.
     * 
     * <p>
     * Converts Post entity to PostDocument and sends to Kafka topic.
     * A consumer service will index it in Elasticsearch.
     * 
     * @param post the post to sync
     * @return Mono that completes when sync message is sent
     */
    private Mono<Void> syncToElasticsearch(Post post) {
        PostDocument document = PostDocument.builder()
                .id(post.getId().toString())
                .postId(post.getId())
                .userId(post.getUserId())
                .content(post.getContent())
                .visibility(post.getVisibility().name())
                .commentsEnabled(post.getCommentsEnabled())
                .isDeleted(post.getIsDeleted())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .shareCount(post.getShareCount())
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();

        if (post.getMedia() != null && !post.getMedia().isEmpty()) {
            List<PostDocument.MediaInfo> mediaInfo = post.getMedia().stream()
                    .map(media -> PostDocument.MediaInfo.builder()
                            .mediaUrl(media.getMediaUrl())
                            .mediaType(media.getMediaType().name())
                            .displayOrder(media.getDisplayOrder())
                            .build())
                    .collect(Collectors.toList());
            document.setMedia(mediaInfo);
        }

        return elasticsearchSyncProducer.syncPostDocument(document);
    }

    /**
     * Maps Post entity to PostResponse DTO.
     * 
     * <p>
     * Converts all fields including transient fields and media attachments.
     * This DTO is returned to clients via REST API.
     * 
     * @param post the post entity
     * @return PostResponse DTO
     */
    private PostResponse mapToResponse(Post post) {
        List<MediaResponse> mediaResponses = null;
        if (post.getMedia() != null) {
            mediaResponses = post.getMedia().stream()
                    .map(media -> MediaResponse.builder()
                            .id(media.getId())
                            .mediaUrl(media.getMediaUrl())
                            .mediaType(media.getMediaType())
                            .displayOrder(media.getDisplayOrder())
                            .width(media.getWidth())
                            .height(media.getHeight())
                            .fileSize(media.getFileSize())
                            .build())
                    .collect(Collectors.toList());
        }

        return PostResponse.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .content(post.getContent())
                .visibility(post.getVisibility())
                .commentsEnabled(post.getCommentsEnabled())
                .allowedCommenters(post.getAllowedCommenters())
                .sharingEnabled(post.getSharingEnabled())
                .isArchived(post.getIsArchived())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .shareCount(post.getShareCount())
                .bookmarkCount(post.getBookmarkCount())
                .viewCount(post.getViewCount())
                .isBookmarkedByCurrentUser(post.getIsBookmarkedByCurrentUser())
                .isSharedByCurrentUser(post.getIsSharedByCurrentUser())
                .media(mediaResponses)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
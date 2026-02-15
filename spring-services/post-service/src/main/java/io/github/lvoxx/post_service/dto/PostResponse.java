package io.github.lvoxx.post_service.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.github.lvoxx.cloudinary_starter.dto.MediaResponse;
import io.github.lvoxx.post_service.model.Post.AllowedCommenters;
import io.github.lvoxx.post_service.model.Post.PostVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Response DTO for post data with HATEOAS support.
 * 
 * <p>
 * This DTO includes:
 * <ul>
 * <li>Basic post information (content, timestamps)</li>
 * <li>Visibility and permission settings</li>
 * <li>Engagement metrics (likes, comments, shares, bookmarks, views)</li>
 * <li>Media attachments</li>
 * <li>Current user's interaction status (bookmarked, shared)</li>
 * <li>HATEOAS links for related operations</li>
 * </ul>
 * 
 * <p>
 * Note: Like count is denormalized for performance. The actual likes
 * are managed by the like-service microservice.
 */
@Data
@ToString(exclude = "content") // Avoid logging full content in toString
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse extends RepresentationModel<PostResponse> {

    /**
     * Unique identifier for the post.
     */
    private Long id;

    /**
     * ID of the user who created the post.
     */
    private Long userId;

    /**
     * Text content of the post.
     */
    private String content;

    /**
     * Visibility level of the post.
     */
    private PostVisibility visibility;

    /**
     * Whether comments are enabled for this post.
     */
    private Boolean commentsEnabled;

    /**
     * Who is allowed to comment on this post.
     */
    private AllowedCommenters allowedCommenters;

    /**
     * Whether sharing is enabled for this post.
     */
    private Boolean sharingEnabled;

    /**
     * Whether the post is archived by the owner.
     */
    private Boolean isArchived;

    /**
     * Number of likes on this post.
     * Updated by like-service via repository method.
     */
    private Long likeCount;

    /**
     * Number of comments on this post.
     * Updated by comment-service via repository method.
     */
    private Long commentCount;

    /**
     * Number of times this post has been shared.
     */
    private Long shareCount;

    /**
     * Number of times this post has been bookmarked.
     */
    private Long bookmarkCount;

    /**
     * Number of times this post has been viewed.
     */
    private Long viewCount;

    /**
     * Whether the current user has bookmarked this post.
     * Null if no user is authenticated.
     */
    private Boolean isBookmarkedByCurrentUser;

    /**
     * Whether the current user has shared this post.
     * Null if no user is authenticated.
     */
    private Boolean isSharedByCurrentUser;

    /**
     * List of media attachments (images/videos).
     */
    private List<MediaResponse> media;

    /**
     * Timestamp when the post was created.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Timestamp when the post was last updated.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
package io.github.lvoxx.post_service.dto;

import java.util.List;

import io.github.lvoxx.post_service.model.Post.AllowedCommenters;
import io.github.lvoxx.post_service.model.Post.PostVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new post.
 * 
 * <p>
 * This DTO contains all necessary information to create a post including:
 * <ul>
 * <li>Post content (required, max 5000 characters)</li>
 * <li>Visibility level (PUBLIC, FOLLOWERS, PRIVATE, CUSTOM)</li>
 * <li>Comment settings (enabled/disabled, who can comment)</li>
 * <li>Sharing settings</li>
 * <li>Optional media attachments (max 10 files)</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {

    /**
     * The text content of the post.
     * Required field, max 5000 characters.
     */
    @NotBlank(message = "{validation.post.content.required}")
    @Size(max = 5000, message = "{validation.post.content.max}")
    private String content;

    /**
     * Post visibility level.
     * Defaults to PUBLIC if not specified.
     */
    @NotNull(message = "{validation.post.visibility.invalid}")
    @Builder.Default
    private PostVisibility visibility = PostVisibility.PUBLIC;

    /**
     * Whether comments are enabled for this post.
     * Defaults to true if not specified.
     */
    @Builder.Default
    private Boolean commentsEnabled = true;

    /**
     * Who is allowed to comment on this post.
     * Defaults to EVERYONE if not specified.
     * Only applies when commentsEnabled is true.
     */
    @Builder.Default
    private AllowedCommenters allowedCommenters = AllowedCommenters.EVERYONE;

    /**
     * Whether this post can be shared by other users.
     * Defaults to true if not specified.
     */
    @Builder.Default
    private Boolean sharingEnabled = true;

    /**
     * Optional list of media attachments.
     * Maximum 10 media files per post.
     */
    @Size(max = 10, message = "{validation.post.media.max-count}")
    private List<MediaUploadRequest> media;
}
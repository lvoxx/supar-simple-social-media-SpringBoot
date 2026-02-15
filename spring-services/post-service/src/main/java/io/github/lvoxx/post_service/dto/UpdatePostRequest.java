package io.github.lvoxx.post_service.dto;

import io.github.lvoxx.post_service.model.Post.AllowedCommenters;
import io.github.lvoxx.post_service.model.Post.PostVisibility;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing post.
 * 
 * <p>
 * All fields are optional. Only provided fields will be updated.
 * The post owner can update:
 * <ul>
 * <li>Content (will be re-validated by AI)</li>
 * <li>Visibility level</li>
 * <li>Comment settings</li>
 * <li>Sharing settings</li>
 * </ul>
 * 
 * <p>
 * Media attachments cannot be updated after creation.
 * To change media, delete and recreate the post.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostRequest {

    /**
     * Updated post content.
     * If provided, will be re-validated by AI.
     */
    @Size(max = 5000, message = "{validation.post.content.max}")
    private String content;

    /**
     * Updated visibility level.
     */
    private PostVisibility visibility;

    /**
     * Enable or disable comments.
     */
    private Boolean commentsEnabled;

    /**
     * Update who can comment.
     */
    private AllowedCommenters allowedCommenters;

    /**
     * Enable or disable sharing.
     */
    private Boolean sharingEnabled;
}
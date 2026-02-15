package io.github.lvoxx.post_service.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sharing a post.
 * 
 * <p>
 * When sharing a post, users can optionally add their own commentary
 * to provide context or express their thoughts about the shared content.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharePostRequest {

    /**
     * Optional content to add when sharing.
     * This is the user's own commentary on the shared post.
     */
    @Size(max = 500, message = "Shared content must not exceed 500 characters")
    private String sharedContent;
}

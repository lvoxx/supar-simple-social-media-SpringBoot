package io.github.lvoxx.post_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * PostShare entity representing a user's share of a post.
 * 
 * <p>
 * When a user shares a post, they can optionally add their own
 * content/commentary.
 * This creates a new post that references the original post.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("post_shares")
public class PostShare {

    @Id
    private Long id;

    @Column("post_id")
    private Long postId;

    @Column("user_id")
    private Long userId;

    /**
     * Optional content added by the user when sharing.
     * This allows users to add their own commentary when sharing someone else's
     * post.
     */
    @Column("shared_content")
    private String sharedContent;

    @Column("created_at")
    private LocalDateTime createdAt;
}
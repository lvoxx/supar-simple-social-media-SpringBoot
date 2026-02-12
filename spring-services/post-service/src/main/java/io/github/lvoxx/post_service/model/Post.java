package io.github.lvoxx.post_service.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Post entity representing a user post with advanced features.
 * 
 * <p>
 * This entity supports:
 * <ul>
 * <li>Multiple visibility levels (PUBLIC, FOLLOWERS, PRIVATE, CUSTOM)</li>
 * <li>Granular comment permissions (EVERYONE, FOLLOWERS, MENTIONED,
 * NOBODY)</li>
 * <li>Share control</li>
 * <li>Archive functionality</li>
 * <li>Media attachments</li>
 * <li>Engagement metrics (likes, comments, shares, bookmarks, views)</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("posts")
public class Post {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("content")
    private String content;

    @Column("visibility")
    @Builder.Default
    private PostVisibility visibility = PostVisibility.PUBLIC;

    @Column("comments_enabled")
    @Builder.Default
    private Boolean commentsEnabled = true;

    @Column("allowed_commenters")
    @Builder.Default
    private AllowedCommenters allowedCommenters = AllowedCommenters.EVERYONE;

    @Column("sharing_enabled")
    @Builder.Default
    private Boolean sharingEnabled = true;

    @Column("is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column("is_archived")
    @Builder.Default
    private Boolean isArchived = false;

    @Column("like_count")
    @Builder.Default
    private Long likeCount = 0L;

    @Column("comment_count")
    @Builder.Default
    private Long commentCount = 0L;

    @Column("share_count")
    @Builder.Default
    private Long shareCount = 0L;

    @Column("bookmark_count")
    @Builder.Default
    private Long bookmarkCount = 0L;

    @Column("view_count")
    @Builder.Default
    private Long viewCount = 0L;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("deleted_at")
    private LocalDateTime deletedAt;

    @Column("archived_at")
    private LocalDateTime archivedAt;

    @Transient
    private List<PostMedia> media;

    @Transient
    private Boolean isBookmarkedByCurrentUser;

    @Transient
    private Boolean isSharedByCurrentUser;

    /**
     * Post visibility levels.
     */
    public enum PostVisibility {
        /** Visible to everyone */
        PUBLIC,
        /** Visible to followers only */
        FOLLOWERS,
        /** Visible to author only */
        PRIVATE,
        /** Visible to custom list of users */
        CUSTOM
    }

    /**
     * Who is allowed to comment on the post.
     */
    public enum AllowedCommenters {
        /** Anyone can comment */
        EVERYONE,
        /** Only followers can comment */
        FOLLOWERS,
        /** Only mentioned users can comment */
        MENTIONED,
        /** Comments disabled */
        NOBODY
    }
}
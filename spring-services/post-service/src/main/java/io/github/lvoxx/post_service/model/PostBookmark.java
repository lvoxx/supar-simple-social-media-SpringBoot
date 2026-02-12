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
 * PostBookmark entity representing a user's bookmark of a post.
 * 
 * <p>
 * Users can save posts to their bookmarks for later reference.
 * Bookmarks can optionally be organized into named collections.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("post_bookmarks")
public class PostBookmark {

    @Id
    private Long id;

    @Column("post_id")
    private Long postId;

    @Column("user_id")
    private Long userId;

    /**
     * Optional collection/folder name for organizing bookmarks.
     * Examples: "Read Later", "Favorites", "Work", "Inspiration"
     */
    @Column("collection_name")
    private String collectionName;

    @Column("created_at")
    private LocalDateTime createdAt;
}
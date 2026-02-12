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
 * PostMedia entity representing media attachments for posts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("post_media")
public class PostMedia {

    @Id
    private Long id;

    @Column("post_id")
    private Long postId;

    @Column("media_url")
    private String mediaUrl;

    @Column("media_type")
    private MediaType mediaType;

    @Column("public_id")
    private String publicId;

    @Column("display_order")
    private Integer displayOrder;

    @Column("width")
    private Integer width;

    @Column("height")
    private Integer height;

    @Column("file_size")
    private Long fileSize;

    @Column("created_at")
    private LocalDateTime createdAt;

    public enum MediaType {
        IMAGE,
        VIDEO
    }
}

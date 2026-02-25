package io.github.lvoxx.media_service.model;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.github.lvoxx.media_service.utils.MediaStatus;
import io.github.lvoxx.media_service.utils.MediaType;
import io.github.lvoxx.media_service.utils.VisibilityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("media_object")
public class MediaObject {

    @Id
    private UUID id;

    @Column("owner_id")
    private String ownerId;

    @Column("media_type")
    private MediaType mediaType;

    @Column("visibility")
    private VisibilityType visibility;

    @Column("width")
    private Integer width;

    @Column("height")
    private Integer height;

    @Column("duration")
    private Integer duration;

    @Column("size_bytes")
    private Long sizeBytes;

    @Column("format")
    private String format;

    @Column("created_at")
    private Instant createdAt;

    @Column("deleted_at")
    private Instant deletedAt;

    @Column("status")
    private MediaStatus status;
}
